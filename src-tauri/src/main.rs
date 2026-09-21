// Casca nativa (Tauri) da Bancada. Sobe o backend Spring embutido (sidecar) com um token de
// sessão, mostra a splash até ele responder e então navega para a interface servida por ele.
// Fechar a janela só a esconde na bandeja: a detecção de dispositivos continua rodando, e um
// aparelho plugado com a janela escondida vira notificação do Windows.
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use std::io::{BufRead, BufReader, Read, Write};
use std::net::TcpStream;
use std::process::{Child, Command};
use std::sync::Mutex;
use std::time::Duration;
use tauri::menu::{Menu, MenuItem};
use tauri::path::BaseDirectory;
use tauri::tray::{MouseButton, MouseButtonState, TrayIconBuilder, TrayIconEvent};
use tauri::webview::DownloadEvent;
use tauri::{AppHandle, Manager, WebviewUrl, WebviewWindowBuilder, WindowEvent};
use tauri_plugin_autostart::{MacosLauncher, ManagerExt};
use tauri_plugin_notification::NotificationExt;

#[cfg(windows)]
use std::os::windows::process::CommandExt;
#[cfg(windows)]
const CREATE_NO_WINDOW: u32 = 0x0800_0000;

const ADDRESS: &str = "127.0.0.1:8747";
const BASE_URL: &str = "http://127.0.0.1:8747";
const READY_ATTEMPTS: u32 = 600;
const READY_INTERVAL: Duration = Duration::from_millis(500);
const EVENTS_RETRY: Duration = Duration::from_secs(5);

/// Processo do backend, encerrado junto com o app.
struct Backend(Mutex<Option<Child>>);

fn port_open() -> bool {
    TcpStream::connect_timeout(&ADDRESS.parse().unwrap(), Duration::from_millis(400)).is_ok()
}

/// O backend só conta como pronto quando responde HTTP 200 em `/`: a porta abre antes de o
/// Spring conseguir servir a página, e navegar nesse intervalo deixa a janela em branco.
fn backend_ready() -> bool {
    let Ok(mut stream) = TcpStream::connect_timeout(&ADDRESS.parse().unwrap(), Duration::from_millis(400)) else {
        return false;
    };
    let _ = stream.set_read_timeout(Some(Duration::from_millis(800)));
    if stream.write_all(b"GET / HTTP/1.0\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n").is_err() {
        return false;
    }
    let mut buffer = [0u8; 64];
    let read = stream.read(&mut buffer).unwrap_or(0);
    read > 0 && String::from_utf8_lossy(&buffer[..read]).contains(" 200")
}

/// Launcher do backend gerado pelo jpackage: `.exe` na raiz no Windows, `bin/` nos outros sistemas.
fn backend_path(app: &tauri::App) -> Option<std::path::PathBuf> {
    #[cfg(windows)]
    let relative = "bancada-backend.exe";
    #[cfg(not(windows))]
    let relative = "bin/bancada-backend";
    for folder in ["resources/bancada-backend/", "bancada-backend/"] {
        if let Ok(path) = app.path().resolve(format!("{folder}{relative}"), BaseDirectory::Resource) {
            if path.exists() {
                return Some(path);
            }
        }
    }
    None
}

fn start_backend(app: &tauri::App, token: &str) -> Option<Child> {
    let executable = backend_path(app)?;
    let mut command = Command::new(executable);
    command.env("BANCADA_TOKEN", token);
    // O backend se encerra sozinho se esta casca sumir sem avisar (gerenciador de tarefas, queda).
    command.arg(format!("--parent-pid={}", std::process::id()));
    #[cfg(windows)]
    command.creation_flags(CREATE_NO_WINDOW);
    command.spawn().ok()
}

fn show_window(app: &AppHandle) {
    if let Some(window) = app.get_webview_window("main") {
        let _ = window.show();
        let _ = window.unminimize();
        let _ = window.set_focus();
    }
}

fn window_hidden(app: &AppHandle) -> bool {
    app.get_webview_window("main")
        .map(|window| !window.is_visible().unwrap_or(false) || window.is_minimized().unwrap_or(false))
        .unwrap_or(true)
}

fn stop_backend(app: &AppHandle) {
    if let Some(state) = app.try_state::<Backend>() {
        if let Ok(mut guard) = state.0.lock() {
            if let Some(mut child) = guard.take() {
                let _ = child.kill();
            }
        }
    }
}

/// Acompanha os eventos do backend (SSE) e avisa pelo Windows quando um dispositivo conecta ou
/// desconecta enquanto a janela está escondida. Com a janela aberta, a própria interface avisa.
fn watch_device_events(app: AppHandle, token: String) {
    std::thread::spawn(move || loop {
        if let Ok(mut stream) = TcpStream::connect(ADDRESS) {
            // HTTP/1.0: resposta sem chunked encoding, então cada linha `data:` chega inteira.
            let request = format!(
                "GET /api/events?token={token} HTTP/1.0\r\nHost: 127.0.0.1\r\nAccept: text/event-stream\r\n\r\n"
            );
            if stream.write_all(request.as_bytes()).is_ok() {
                let reader = BufReader::new(stream);
                for line in reader.lines() {
                    let Ok(line) = line else { break };
                    let Some(payload) = line.strip_prefix("data:") else { continue };
                    let Ok(event) = serde_json::from_str::<serde_json::Value>(payload) else { continue };
                    let kind = event["type"].as_str().unwrap_or_default();
                    let title = match kind {
                        "DISCOVERED" => "Novo dispositivo conectado",
                        "ONLINE" => "Dispositivo conectado",
                        "OFFLINE" => "Dispositivo desconectado",
                        _ => continue,
                    };
                    if window_hidden(&app) {
                        let body = event["message"].as_str().unwrap_or_default().to_string();
                        let _ = app.notification().builder().title(title).body(body).show();
                    }
                }
            }
        }
        std::thread::sleep(EVENTS_RETRY);
    });
}

fn main() {
    tauri::Builder::default()
        .plugin(tauri_plugin_single_instance::init(|app, _args, _cwd| show_window(app)))
        .plugin(tauri_plugin_autostart::init(MacosLauncher::LaunchAgent, None))
        .plugin(tauri_plugin_notification::init())
        .setup(|app| {
            if let Err(error) = app.autolaunch().enable() {
                eprintln!("autostart: não foi possível habilitar o início automático: {error}");
            }

            let token = format!("{}{}", uuid::Uuid::new_v4().simple(), uuid::Uuid::new_v4().simple());
            // Backend já rodando (desenvolvimento): reaproveita. Senão, sobe o embutido.
            let child = if port_open() { None } else { start_backend(app, &token) };
            app.manage(Backend(Mutex::new(child)));

            let open = MenuItem::with_id(app, "open", "Abrir", true, None::<&str>)?;
            let quit = MenuItem::with_id(app, "quit", "Sair", true, None::<&str>)?;
            let menu = Menu::with_items(app, &[&open, &quit])?;
            let mut tray = TrayIconBuilder::new()
                .tooltip("Bancada")
                .menu(&menu)
                .show_menu_on_left_click(false)
                .on_menu_event(|app, event| match event.id().as_ref() {
                    "open" => show_window(app),
                    "quit" => {
                        stop_backend(app);
                        app.exit(0);
                    }
                    _ => {}
                })
                .on_tray_icon_event(|tray, event| {
                    if let TrayIconEvent::Click {
                        button: MouseButton::Left,
                        button_state: MouseButtonState::Up,
                        ..
                    } = event
                    {
                        show_window(tray.app_handle());
                    }
                });
            if let Some(icon) = app.default_window_icon() {
                tray = tray.icon(icon.clone());
            }
            tray.build(app)?;

            let window = WebviewWindowBuilder::new(app, "main", WebviewUrl::App("index.html".into()))
                .title("Bancada")
                .inner_size(1360.0, 880.0)
                .min_inner_size(1024.0, 640.0)
                .center()
                // Sem isto o WebView2 descarta downloads (arquivos do dispositivo, backups).
                .on_download(|webview, event| {
                    if let DownloadEvent::Requested { destination, .. } = event {
                        let file_name = destination
                            .file_name()
                            .map(|name| name.to_os_string())
                            .unwrap_or_else(|| std::ffi::OsString::from("download"));
                        if let Ok(folder) = webview.path().download_dir() {
                            *destination = folder.join(file_name);
                        }
                    }
                    true
                })
                .build()?;

            let handle = app.handle().clone();
            std::thread::spawn(move || {
                let mut ready = false;
                for _ in 0..READY_ATTEMPTS {
                    if backend_ready() {
                        ready = true;
                        break;
                    }
                    std::thread::sleep(READY_INTERVAL);
                }
                if !ready {
                    return;
                }
                if let Ok(url) = format!("{BASE_URL}/?token={token}").parse::<tauri::Url>() {
                    let _ = window.navigate(url);
                }
                watch_device_events(handle, token);
            });
            Ok(())
        })
        .on_window_event(|window, event| match event {
            WindowEvent::CloseRequested { api, .. } => {
                api.prevent_close();
                let _ = window.hide();
            }
            WindowEvent::Destroyed => stop_backend(window.app_handle()),
            _ => {}
        })
        .run(tauri::generate_context!())
        .expect("erro ao iniciar a Bancada");
}
