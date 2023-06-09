package Handler;

import util.RollBack;
import util.Utils;
import util.dao.subDao.GroupDao;
import util.dao.subDao.UserDao;

import java.io.*;
import java.net.Socket;

/**
 * @auther 齿轮
 * @create 2023-05-21-22:54
 */
public class HandlerImpl implements Handler {
    private Socket ClientSocket;
    private String username;
    private RollBack rollBack;
    private UserDao userDao=new UserDao();
    private GroupDao groupDao=new GroupDao();

    @Override
    public void handler() throws IOException {
        InputStream inputStream = ClientSocket.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        Utils.TempString.put(username, new StringBuilder());
        while ((line = br.readLine()) != null) {
            if (line.equals("OneLineMessage")) {
                OneLineMessage(br);
            } else if (line.equals("File")) {
                File(br);
            } else if (line.equals("addFriend")) {
                addFriend(br);
            }else if(line.equals("createGroup")){
                createGroup(br);
            } else if (line.equals("joinGroup")) {
                joinGroup(br);
                System.out.println(1);
            }else if (line.equals("getAllContentMsg")) {
                getAllContentMsg(br);
            }
        }
    }

    private void joinGroup(BufferedReader br)throws IOException{
        String line;
        String joinGroupName=null;  //群聊名字
        int count=0;
        while((line=br.readLine())!=null){
            if(count==0){
                count++;
                joinGroupName=line;
                //数据库加入群聊
                groupDao.joinGroup(username,joinGroupName);
                System.out.println("加入群聊的消息:"+username+"已加入群聊 "+joinGroupName);
                StringBuilder stringBuilder = Utils.TempString.get(username);//将信息存储在源用户名中
                stringBuilder.append(username+"已加入群聊 "+joinGroupName);

            } else if (count==1 && line.equals("bye")) {
                //接收完毕，开始发送
                rollBack.joinGroup(username,joinGroupName);
                //发送完毕，清空缓存
                Utils.TempString.put(username, new StringBuilder());
                return;

            }
        }
    }
    private void createGroup(BufferedReader br)throws IOException{
        String line;
        String groupName=null;  //群聊名字
        int count=0;
        while((line=br.readLine())!=null){
            if(count==0){
                count++;
                groupName=line;
                //数据库创建群聊
                userDao.createGroup(username,groupName);
                System.out.println("创建群聊的消息:"+username+"已创建群聊 "+groupName);
                StringBuilder stringBuilder = Utils.TempString.get(username);//将信息存储在源用户名中
                stringBuilder.append(username+"已创建群聊 "+groupName);

            } else if (count==1 && line.equals("bye")) {
                //接收完毕，开始发送
                rollBack.createGroup(username,groupName);
                //发送完毕，清空缓存
                Utils.TempString.put(username, new StringBuilder());
                return;

            }else if (line.equals("getAllContentMsg")) {
                getAllContentMsg(br);
            }
        }
    }

    private void getAllContentMsg(BufferedReader br)throws IOException {
        String line;
        String send_user = null;
        String target_user = null;
        int count = 0;
        while ((line = br.readLine()) != null) {
            if (count == 0) {
                count++;
                send_user = line;
            } else if (count == 1) {
                count++;
                target_user = line;
        }else if (count == 2 && line.equals("bye")) {
            //接收完毕，开始发送
            rollBack.getAllContentMsg(send_user, target_user);
            //发送完毕，清空缓存
            Utils.TempString.put(username, new StringBuilder());
            return;
        }


    }
    }

    private void addFriend(BufferedReader br)throws IOException {
        String line;
        String friendName = null;
        int count = 0;
        while ((line = br.readLine()) != null) {
            if (count == 0) {
                count++;
                friendName = line;//获得好友名字
                System.out.println("好友名字："+friendName);
            } else if (count == 1) {
                count++;
                StringBuilder stringBuilder = Utils.TempString.get(username);//将信息存储在源用户名中
                System.out.println("添加好友的消息:"+username+"请求"+line);
                stringBuilder.append(username+"请求"+line);
                //在数据库中添加好友
                userDao.addFriend(username,friendName);
            } else if (count == 2 && line.equals("bye")) {
                //接收完毕，开始发送
                rollBack.addFriend(username, friendName);
                //发送完毕，清空缓存
                Utils.TempString.put(username, new StringBuilder());
                return;
            }
        }
    }

    private void File  (BufferedReader br) throws IOException {
        String line;
        String name = null;
        String fileName = null;
        String length = null;
        int count = 0;
        while ((line = br.readLine()) != null) {
            if (count == 0) {
                count++;
                name = line;//获得目的用户名
                System.out.println(name);
            } else if (count == 1) {
                count++;
                fileName = line;
                System.out.println(fileName);
            } else if (count == 2) {
                count++;
                length = line;
                System.out.println(length);
                //获得文件名后立即开始接受
                fileName = Utils.storePath + username + "+" + fileName;
                ReceiveFile(fileName, length);
            } else if (count == 3 && line.equals("bye")) {
                System.out.println(line);
                rollBack.SendFile(username, name, fileName);
                return;
            }
        }
    }

    private void ReceiveFile(String fileName, String length) {
        long Length = Long.parseLong(length);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(fileName);
            byte[] buffer;
            if (Length < 1024) {
                buffer = new byte[(int) Length];
            } else {
                buffer = new byte[1024];
            }
            int len;
            long totalLen = 0;
            InputStream inputStream = ClientSocket.getInputStream();
            while ((len = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
                totalLen += len;
                System.out.println(totalLen+":"+buffer);
                if (totalLen == Length) {
                    System.out.println("served："+totalLen);
                    break;
                }else if (Length - totalLen < buffer.length && (Length - totalLen < 1024)) {//Length - totalLen为接收区还需要接受的剩余量
                    buffer = new byte[(int) (Length - totalLen)];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void OneLineMessage(BufferedReader br) throws IOException {
        String line;
        String name = null;
        int count = 0;
        while ((line = br.readLine()) != null) {
            if (count == 0) {
                count++;
                name = line;//获得目的用户名
                System.out.println(name);
            } else if (count == 1) {
                count++;
                StringBuilder stringBuilder = Utils.TempString.get(username);//将信息存储在源用户名中
                System.out.println(line);
                System.out.println(line);
                rollBack.addContentMsg(username,name,line);
                stringBuilder.append(line);
            } else if (count == 2 && line.equals("bye")) {
                //接收完毕，开始发送
                rollBack.SendOneLineMessage(username, name);
                //发送完毕，清空缓存
                Utils.TempString.put(username, new StringBuilder());
                return;
            }
        }
    }

    public HandlerImpl(Socket clientSocket, String username, RollBack rollBack) {
        ClientSocket = clientSocket;
        this.username = username;
        this.rollBack = rollBack;
    }
}
