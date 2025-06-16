import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Asynchronous task for securely uploading files to an FTPS server.
 * Supports conditional logic for skipping, replacing, or deleting files based on their type and existence.
 */
public class FtpUploadTask extends AsyncTask<String, Void, Void> {

    private static final String TAG = "FtpUploader";

    private Context context;
    private File localDir;
    private String ftpDir;
    private String ftpURL;
    private String ftpUsername;
    private String ftpPassword;

    public FtpUploadTask(Context context, File localDir, String ftpDir, String ftpURL, String ftpUsername, String ftpPassword) {
        this.context = context;
        this.localDir = localDir;
        this.ftpDir = ftpDir;
        this.ftpURL = ftpURL;
        this.ftpUsername = ftpUsername;
        this.ftpPassword = ftpPassword;
    }

    @Override
    protected Void doInBackground(String... params) {
        try {
            // Step 1: Connect to FTPS server
            FTPSClient ftps = new FTPSClient("TLS", false);
            ftps.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
            ftps.connect(ftpURL, 21);
            boolean loggedIn = ftps.login(ftpUsername, ftpPassword);
            ftps.execPBSZ(0);
            ftps.execPROT("P");
            ftps.enterLocalPassiveMode();
            ftps.setFileType(FTPClient.BINARY_FILE_TYPE);
            LogUtil.d(TAG, "Login Status : " + loggedIn);

            // Step 2: Read local files from directory
            File[] files = localDir.listFiles();
            if (files == null) {
                LogUtil.e(TAG, "No files found for upload.");
                return null;
            }

            for (File file : files) {
                if (file.isFile()) {
                    String remotePath = ftpDir + "/" + file.getName();
                    boolean exists = ftps.listFiles(remotePath).length > 0;

                    // Step 3a: If file already exists on server
                    if (exists) {
                        // Only re-upload non-database files
                        if (!file.getName().endsWith(".db")) {
                            try (FileInputStream fis = new FileInputStream(file)) {
                                ftps.storeFile(remotePath, fis);
                                int code = ftps.getReplyCode();
                                String msg = ftps.getReplyString();
                                LogUtil.d(TAG, "File reupload : " + file.getName() + " Result " + code + " " + msg);
                            }
                        } else {
                            // Delete older backup database files
                            File newestBackup = getNewestBackup(localDir);
                            if (!file.equals(newestBackup)) {
                                file.delete();
                            }
                        }

                        // Delete .json files even if they exist remotely
                        if (file.getName().endsWith(".json")) {
                            file.delete();
                        }
                    } else {
                        // Step 3b: If file does not exist, upload it
                        try (FileInputStream fis = new FileInputStream(file)) {
                            ftps.storeFile(remotePath, fis);
                            int code = ftps.getReplyCode();
                            String msg = ftps.getReplyString();
                            LogUtil.d(TAG, "File upload : " + file.getName() + " " + code + " " + msg);
                        }

                        // Delete .json file after upload
                        if (file.getName().endsWith(".json")) {
                            file.delete();
                        }
                    }
                }
            }

            // Step 4: Disconnect from FTPS server
            ftps.logout();
            ftps.disconnect();

        } catch (Exception e) {
            LogUtil.e(TAG, "FTP Upload Error : " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets the newest file (by last modified time) in a given directory.
     *
     * @param backupDir Directory to scan
     * @return Most recently modified file or null if not found
     */
    private File getNewestBackup(File backupDir) {
        if (backupDir == null || !backupDir.exists() || !backupDir.isDirectory()) {
            return null;
        }

        File[] backupFiles = backupDir.listFiles();
        if (backupFiles == null || backupFiles.length == 0) {
            return null;
        }

        return Arrays.stream(backupFiles)
                .filter(File::isFile)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);
    }
}
