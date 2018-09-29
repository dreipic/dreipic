package org.dreipic.gui.exp;

import java.awt.BorderLayout;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.xml.bind.DatatypeConverter;

import org.dreipic.gui.SwingUtils;
import org.dreipic.struct.StructMeta;
import org.dreipic.struct.StructMetaData;
import org.dreipic.struct.StructMetaPath;
import org.dreipic.struct.StructPathType;

import com.google.common.base.Throwables;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

final class FilesWindow {
    private final FtpCredentials creds;
    private final byte[] dataKey;

    private final Multimap<String, StructMetaPath> pathMap;
    private final Map<String, DataRec> dataMap;

    private final JTextField txtSearch;
    private final DefaultListModel<String> pathsModel;
    private final JList<String> lstPaths;
    private final DefaultListModel<PathWrapper> versionsModel;
    private final JList<PathWrapper> lstVersions;
    private final JTextField txtDstDir;
    private final JCheckBox chkExtract;
    private final JButton btnDownload;
    private final LogPanel logPanel;

    private boolean downloading;

    private FilesWindow(FtpCredentials creds, byte[] dataKey, List<StructMeta> metas) {
        this.creds = creds;
        this.dataKey = dataKey;

        pathMap = HashMultimap.create();
        for (StructMeta meta : metas) {
            for (StructMetaPath path : meta.paths) {
                if (path.type == StructPathType.FILE || path.type == StructPathType.ARCHIVE) {
                    pathMap.put(path.path, path);
                }
            }
        }

        dataMap = new HashMap<>();
        for (StructMeta meta : metas) {
            for (StructMetaData data : meta.datas) {
                String hashStr = DatatypeConverter.printHexBinary(data.hash);
                dataMap.put(hashStr, new DataRec(meta, data));
            }
        }

        txtSearch = new JTextField();

        pathsModel = new DefaultListModel<>();

        lstPaths = new JList<>(pathsModel);

        versionsModel = new DefaultListModel<>();
        lstVersions = new JList<>(versionsModel);
        lstVersions.setVisibleRowCount(6);

        txtDstDir = new JTextField();
        SwingUtils.onTextChanged(txtDstDir, this::onDstPathChange);

        chkExtract = new JCheckBox("Extract");
        chkExtract.setSelected(true);

        btnDownload = SwingUtils.newButton("Download", false, true, this::onDownloadClick);

        JPanel listsPanel = new JPanel(new BorderLayout());
        listsPanel.add(new JScrollPane(lstPaths), BorderLayout.CENTER);
        listsPanel.add(new JScrollPane(lstVersions), BorderLayout.SOUTH);

        JPanel dlPanel = new JPanel();
        dlPanel.setLayout(new BoxLayout(dlPanel, BoxLayout.X_AXIS));
        dlPanel.add(txtDstDir);
        dlPanel.add(chkExtract);
        dlPanel.add(btnDownload);

        logPanel = new LogPanel(10);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(listsPanel, BorderLayout.CENTER);
        centerPanel.add(dlPanel, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(txtSearch, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(logPanel.getComponent(), BorderLayout.SOUTH);

        JFrame frame = SwingUtils.createFrame("Files", false, panel);
        frame.setVisible(true);

        txtSearch.addActionListener(__ -> onSearch());
        lstPaths.addListSelectionListener(__ -> onPathSelectionChange());
        lstVersions.addListSelectionListener(__ -> onVersionSelectionChange());

        updatePaths();
    }

    static void show(FtpCredentials creds, byte[] dataKey, List<StructMeta> metas) {
        new FilesWindow(creds, dataKey, metas);
    }

    private void onSearch() {
        updatePaths();
    }

    private void onDstPathChange() {
        updateDownloadButton();
    }

    private void onPathSelectionChange() {
        btnDownload.setEnabled(false);
        versionsModel.clear();

        String path = lstPaths.getSelectedValue();
        if (path == null) {
            return;
        }

        List<StructMetaPath> paths = new ArrayList<>(pathMap.get(path));
        Collections.sort(paths, (p1, p2) -> {
            long t1 = p1.time == null ? 0 : p1.time;
            long t2 = p2.time == null ? 0 : p2.time;
            return -Long.compare(t1, t2);
        });

        List<PathWrapper> wrappers = new ArrayList<>();

        for (StructMetaPath metaPath : paths) {
            String hashStr = DatatypeConverter.printHexBinary(metaPath.hash);
            DataRec dataRec = dataMap.get(hashStr);
            PathWrapper wrapper = new PathWrapper(dataRec == null ? null : dataRec.meta, metaPath, dataRec == null ? null : dataRec.data);
            wrappers.add(wrapper);
        }

        Collections.sort(wrappers);
        Collections.reverse(wrappers);

        for (PathWrapper wrapper : wrappers) {
            versionsModel.addElement(wrapper);
        }

        if (!paths.isEmpty()) {
            lstVersions.setSelectedIndex(0);
        }
    }

    private void onVersionSelectionChange() {
        updateDownloadButton();
    }

    private void onDownloadClick() {
        if (downloading) {
            return;
        }

        PathWrapper wrapper = lstVersions.getSelectedValue();
        if (wrapper == null) {
            return;
        }

        File dstDir = new File(txtDstDir.getText());
        boolean extract = chkExtract.isSelected();

        Thread thread = new Thread(() -> download(wrapper, dstDir, extract));
        thread.setName("Download");
        thread.setDaemon(true);
        thread.start();

        btnDownload.setEnabled(false);
        downloading = true;
    }

    private void download(PathWrapper wrapper, File dstDir, boolean extract) {
        try {
            logPanel.clear();
            ExplorerDownload.download(creds, dataKey, wrapper.meta, wrapper.path, wrapper.data, dstDir, extract, logPanel);
        } catch (Throwable e) {
            String s = Throwables.getStackTraceAsString(e);
            logPanel.log("ERROR: %s", s);
        } finally {
            SwingUtilities.invokeLater(() -> {
                downloading = false;
                updateDownloadButton();
            });
        }
    }

    private void updatePaths() {
        String search = txtSearch.getText();

        List<String> allPaths = new ArrayList<>();
        for (String path : pathMap.keySet()) {
            if (path.contains(search)) {
                allPaths.add(path);
            }
        }

        Collections.sort(allPaths);

        pathsModel.clear();
        for (String path : allPaths) {
            pathsModel.addElement(path);
        }

        btnDownload.setEnabled(false);
    }

    private void updateDownloadButton() {
        PathWrapper wrapper = lstVersions.getSelectedValue();
        String path = txtDstDir.getText();
        File file = new File(path);
        btnDownload.setEnabled(!downloading && wrapper != null && wrapper.data != null && file.isDirectory());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FilesWindow.show(null, null, Collections.emptyList());
        });
    }

    private static final class DataRec {
        final StructMeta meta;
        final StructMetaData data;

        DataRec(StructMeta meta, StructMetaData data) {
            this.meta = meta;
            this.data = data;
        }
    }

    private static final class PathWrapper implements Comparable<PathWrapper> {
        final StructMeta meta;
        final StructMetaPath path;
        final StructMetaData data;

        PathWrapper(StructMeta meta, StructMetaPath path, StructMetaData data) {
            this.meta = meta;
            this.path = path;
            this.data = data;
        }

        @Override
        public int compareTo(PathWrapper o) {
            long t1 = meta == null ? 0 : meta.timestamp;
            long t2 = o.meta == null ? 0 : o.meta.timestamp;
            return Long.compare(t1, t2);
        }

        @Override
        public String toString() {
            Date date = new Date(meta == null ? 0 : meta.timestamp);
            String sizeStr = data == null ? "<missing>" : String.format("%,d", data.size);
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS").format(date) + " " + path.type + " " + sizeStr;
        }
    }
}
