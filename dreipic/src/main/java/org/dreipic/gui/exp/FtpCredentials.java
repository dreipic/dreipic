package org.dreipic.gui.exp;

final class FtpCredentials {
    final String host;
    final int port;
    final String login;
    final String password;

    FtpCredentials(String host, int port, String login, String password) {
        this.host = host;
        this.port = port;
        this.login = login;
        this.password = password;
    }
}
