# 📤 Android / Java / FTP Upload Task

This component provides an asynchronous FTPS file uploader for Android-based applications using `AsyncTask`.  
It uploads all files from a local directory to a specified FTPS server and handles re-uploading, skipping, or deleting files based on rules.

---

## 🔧 Features

- ✅ Uploads files to a secure FTPS (TLS) server  
- ✅ Automatically skips existing `.db` files unless it's the latest backup  
- ✅ Deletes uploaded `.json` files to keep the local directory clean  
- ✅ Maintains only the latest `.db` backup file locally  
- ✅ Passive mode and TLS encryption support via Apache Commons Net  

---

## 🚀 How It Works

1. Establishes a secure FTPS connection using TLS
2. Iterates through all files in the given local directory
3. For each file:
   - If already exists on FTP:
     - Re-uploads it if not a `.db` file
     - Keeps only the latest `.db` file, deletes the rest
     - Deletes `.json` files after checking
   - If it does not exist:
     - Uploads it directly
     - Deletes `.json` files after successful upload
4. Closes the FTPS connection

---

## 🧑‍💻 Usage

```java
FtpUploadTask uploader = new FtpUploadTask(
    context,
    new File("/local/path"),
    "/ftp/remote/dir",
    "ftp.example.com",
    "username",
    "password"
);

uploader.execute();
```
| File Type | Upload Condition    | Post-upload behavior        |
| --------- | ------------------- | --------------------------- |
| `.db`     | Only newest is kept | Old `.db` files are deleted |
| `.json`   | Always uploaded     | Deleted after upload        |
| Others    | Always uploaded     | Kept                        |


🔐 Security
Uses FTPS with TLS encryption (execPROT("P"))
Passive mode enabled for firewall-friendly communication
Binary file type set for all transfers

⚠️ Requirements
Apache Commons Net 3.x dependency
Android Context
Internet permission in AndroidManifest.xml:

```java
<uses-permission android:name="android.permission.INTERNET" />
```

📜 License
MIT License — © 2025 Impintell


