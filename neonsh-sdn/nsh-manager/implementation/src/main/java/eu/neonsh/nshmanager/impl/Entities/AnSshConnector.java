package eu.neonsh.nshmanager.impl.Entities;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import eu.neonsh.nshmanager.impl.SshConnectionInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * An SSH connector class that provides the basic configuration options for new SSH connections.
 * Also provides the methods to execute remote commands on the remote SSH servers.
 */
public class AnSshConnector {

    /**
     * Method to connect to a remote host using SSH and execute a given command
     * @param username {@link String}- username of remote hots
     * @param password {@link String}- password of the remote host
     * @param targetIp {@link String}- Ip address of the remote host
     * @param port {@link Integer}- port to connect on, usually 22
     * @param commandToExecute {@link String} - Command to execute on remote host
     * @return {@link String} - result of the executed command in String
     */
    public static String connectTossh(String username, String password, String targetIp, int port, String commandToExecute) {

        try {

            JSch jsch = new JSch();
            Session session = jsch.getSession(username, targetIp, port);
            session.setPassword(password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();

            //open channel
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            //set the command and connect
            channel.setCommand(commandToExecute);
            channel.connect();

            //read the input back from channel
            BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));

            // string to be returned
            String result = "";
            String msg = null;

            //keep adding to result each line of response
            while ((msg = in.readLine()) != null) {
                result  += msg;
            }


            int exitStatus = channel.getExitStatus();
            channel.disconnect();
            session.disconnect();
            //some debugging
            if(exitStatus < 0){
                System.out.println("Done, but exit status not set!");
            }
            else if(exitStatus > 0){
                System.out.println("Done, but with error!");
            }
            else{
                System.out.println("Done!");
            }

            in.close();
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "Exception";
        }

    }

    public static String executeCommand(String commandToExecute, String userName, String password, String IP, int port) {
        try{
            JSch jsch = new JSch();
            Session session = jsch.getSession(userName, IP, port);
            session.setPassword(password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            //open channel
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            //set the command and connect
            channel.setCommand(commandToExecute);
            channel.connect();

            //read the input back from channel
            BufferedReader in = new BufferedReader(new InputStreamReader(channel.getInputStream()));

            // string to be returned
            String result = "";
            String msg = null;

            //keep adding to result each line of response
            while ((msg = in.readLine()) != null) {
                result  += msg;
            }


            int exitStatus = channel.getExitStatus();
            channel.disconnect();
//            session.disconnect();
            //some debugging

            if(exitStatus < 0){
                System.out.println("Done, but exit status not set!");
            }
            else if(exitStatus > 0){
                System.out.println("Done, but with error!");
            }
            else{
                System.out.println("Done!");
            }

            in.close();
            session.disconnect();
            return result;

        } catch (Exception e) {
            e.printStackTrace();
            return "Exception";
        }
    }
}