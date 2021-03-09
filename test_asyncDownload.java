public class DownloadAsyncTask extends SAsyncTaskBase {
    static private final int TIMEOUT_READ = 1000*60;
    static private final int TIMEOUT_CONNECT = 1000*60*20;

    InputStream inputStream ;
    FileOutputStream fileOutputStream ;
    private boolean isWifiOnly = false;

    public DownloadAsyncTask(ArrayList<SObject> downloadList, Context context,String rootPath,boolean isWifiOnly){
        this.packageList = downloadList;
        this.context = context;
        this.rootPath = rootPath;
        this.isWifiOnly = isWifiOnly;
        if(downloadList != null){
            this.progressMax = downloadList.size();
        }
        this.progressCount = 0;
    }

    @Override
    protected Boolean doInBackground(Integer... value){
        sLog.debug("Runnable Download running");
        if(packageList == null || packageList.size() == 0){
            sLog.debug("Error:No Download Target");
            if(downloadProgressListener != null) {
                downloadProgressListener.onResult(PROCESS_CODE_DOWNLOAD, packageList, false,1400);
            }
            return false;
        }
        boolean isSuccessAll = true;
        DATA sdata = DATA.getInstance();

        for (SPackageObject downloadData:packageList) {
            String saveFileName = rootPath+downloadData.filename;
            boolean isSuccess = true;
            long total;
            if(downloadData.status == DOWNLOAD_STATUS_CODE_DOWNLOADED || downloadData.status == DOWNLOAD_STATUS_CODE_INSTALLING || downloadData.status == DOWNLOAD_STATUS_CODE_INSTALL_FAILD){
                progressCount ++;
                continue;
            }

            File temporaryFile = new File (saveFileName);
            if( temporaryFile.exists () )
            {
                total = (int) temporaryFile.length ();
            }
            else {
                total = 0;
                
                try { temporaryFile.createNewFile (); }
                catch( IOException exception ) {
                    exception.printStackTrace ();
                }
            }

           
            if(isWifiOnly && !NetUtility.isWifiConnected(context)){
                isSuccessAll = false;
                continue;
            }

            int fileSize = 0;
            try{

                downloadData.status = DOWNLOAD_STATUS_CODE_DOWNLOADING;
                
                if(downloadProgressListener != null){
                    downloadProgressListener.onStartTask(PROCESS_CODE_DOWNLOAD,progressMax,progressCount,downloadData.name);
                }

                sLog.debug("download "+downloadData.url + "   to:"+saveFileName+"   total:"+total);
                byte[] buffer = new byte[8192];
                int len = 0;

                int progress = -1;

                URL url = new URL(downloadData.url);
                URLConnection urlConnection = url.openConnection();
                urlConnection.connect ();
                fileSize = urlConnection.getContentLength ();

                if(fileSize > total) {
                    url = new URL(downloadData.url);
                    HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
                    httpUrlConnection.setReadTimeout(TIMEOUT_READ);
                    httpUrlConnection.setConnectTimeout(TIMEOUT_CONNECT);
                    httpUrlConnection.setRequestProperty(
                            "Range",
                            String.format("bytes=%d-%d", total, fileSize)
                    );
                    sLog.debug("Range:" + String.format("bytes=%d-%d", total, fileSize));
                    httpUrlConnection.connect();
                    inputStream = httpUrlConnection.getInputStream();
                    fileOutputStream = new FileOutputStream(temporaryFile, true);

                    while ((len = inputStream.read(buffer)) != -1 && !forceCancel) {
                        total += len;
                        int curProgress = (int) (total * 100.f / fileSize);
                        if (curProgress != progress) {
                            progress = curProgress;
                            sLog.debug("Download progress : " + progress + "%" + "  len:" + len);
                            if (downloadProgressListener != null) {
                                downloadProgressListener.onMakeProgress(PROCESS_CODE_DOWNLOAD, progress);
                            }
                        }
                        fileOutputStream.write(buffer, 0, len);
                    }
                    if (forceCancel) {
                        setResumeProgress(total);
                        return false;
                    }
                    fileOutputStream.close();
                    inputStream.close();
                }
                downloadData.status =.DOWNLOAD_STATUS_CODE_DOWNLOADED;
                progressCount ++;

            }catch (Exception e){

                if (e instanceof SocketTimeoutException) {
                    Intent intent = new Intent(.ROCESS_NO_INTERNET_ACCESS_BROADCAST);
                    intent.putExtra(INTENT_EXTRA_ERROR_CODE, ERROR_CODE_NO_INTERNET_ACCESS);
                    sLog.debug(e.getMessage());
                    SCSApplication.getInstance().sendBroadcast(intent);
                }

                long availableSize = Utility.getAvailableSize(rootPath);                          
                sLog.debug("availableSize:"+availableSize + "   totalFileSize:"+fileSize);
                if(fileSize > availableSize) {
                    //error:low storage
                    responseCode = RESPONSE_CODE_ERROR_STORAGE;
                    return false;
                }

                setResumeProgress(total);
                e.printStackTrace();
                isSuccess = false;
                isSuccessAll = false;
                downloadData.status = DOWNLOAD_STATUS_CODE_DOWNLOAD_FAILD;
            }

            
            if(downloadProgressListener != null){
                downloadProgressListener.onFinishProgress(AppUpdateService.PROCESS_CODE_DOWNLOAD,progressMax,progressCount,downloadData.name,isSuccess);
            }

        }


        return isSuccessAll;
    }
    
    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);
        
        if(downloadProgressListener != null) {
            downloadProgressListener.onResult(PROCESS_CODE_DOWNLOAD, packageList, result,responseCode);
        }
    }

    private void setResumeProgress(long size){
        if(size == 0 || inputStream == null || fileOutputStream == null){
            return;
        }
        try {
            fileOutputStream.close();
            inputStream.close();
        }catch (Exception e){}
    }

}
