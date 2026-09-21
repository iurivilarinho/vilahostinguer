package com.bancada.records;

import com.jcraft.jsch.Channel;

/** PTY opened for a web terminal, and the name of the thread that pumps its output. */
public record ShellSession(Channel channel, String threadName) {
}
