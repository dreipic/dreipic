package org.dreipic.gui.exp;

import java.util.List;

import org.dreipic.struct.StructMeta;

final class ConnectDetails {
    final FtpCredentials credentials;
    final byte[] dataKey;
    final List<StructMeta> metas;

    ConnectDetails(FtpCredentials credentials, byte[] dataKey, List<StructMeta> metas) {
        this.credentials = credentials;
        this.dataKey = dataKey;
        this.metas = metas;
    }
}
