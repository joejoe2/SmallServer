# SmallServer

測試環境: win10, android 8
server的網域nctu.me停止服務了，正在找替代的

app和server採用socket來進行所有傳輸，當app連入時server會傳回一份檔案列表，app藉由此表在畫面上顯示出所有檔案和資料夾供使用者瀏覽，使用者可選擇下載選取的檔案或是上傳手機內的檔案到server的upload資料夾中。

功能:
server side: view,info,download,upload
client side app: view,download,upload
