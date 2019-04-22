/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javax.cldc.io;

import java.io.IOException;

/**
 * block and non block socket, setoption() to set it attribute  <code>
 *    void t13() {
 *        try {
 *            Socket conn = (Socket) Connector.open("socket://baidu.com:80");
 *            conn.setOption(Socket.OP_TYPE_NON_BLOCK, Socket.OP_VAL_NON_BLOCK);
 *            String request = "GET / HTTP/1.1\r\n\r\n";
 *            conn.write(request.getBytes(), 0, request.length());
 *            byte[] rcvbuf = new byte[256];
 *            int len = 0;
 *            while (len != -1) {
 *                len = conn.read(rcvbuf, 0, 256);
 *                for (int i = 0; i < len; i++) {
 *                    System.out.print((char) rcvbuf[i]);
 *                }
 *                System.out.print("\n");
 *            };
 *        } catch (Exception e) {
 *
 *        }
 *    }
 * </code>
 *
 * @author gust
 */
public interface SocketConnection extends StreamConnection {

    /**
     * 非阻塞写，返回写长度
     *
     * @param b
     * @param off
     * @param len
     * @return
     * @throws IOException
     */
    public int write(byte b[], int off, int len)
            throws IOException;
    
    
    /**
     * 非阻塞读，返回读长度
     *
     * @param b
     * @param off
     * @param len
     * @return
     * @throws IOException
     */
    int read(byte b[], int off, int len)
            throws IOException;

    /**
     * 设置阻塞或非阻塞属性
     *
     * @param type
     * @param val
     * @param val2
     */
    void setOption(int type, int val, int val2);
}
