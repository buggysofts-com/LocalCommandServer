import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.Optional;

public class MAIN extends Application {

    private final int ACTIVITY_BROADCAST_PORT_OWN=1817;
    private final int ACTIVITY_SCANNER_PORT_NOT_OWN=7181;
    private final int CONNECTION_PORT_OWN=4921;
    private final int CONNECTION_PORT_NOT_OWN=1294;

    private DatagramSocket ActivityBroadCastSoc= null;
    private DatagramSocket CommandSoc=null;

    public static void main(String[] args) {
        System.out.println(new File(".").getAbsolutePath());
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

        InitializeAppData();

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    ActivityBroadCastSoc = new DatagramSocket(ACTIVITY_BROADCAST_PORT_OWN);
                } catch (SocketException e) {
                    e.printStackTrace();
                }

                while (true){

                    InetAddress MyAddr=null;
                    try {
                        MyAddr=InetAddress.getLocalHost();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    InetAddress BroadcastAddr=null;
                    try {
                        BroadcastAddr=NetworkInterface.getByInetAddress(MyAddr).
                                getInterfaceAddresses().get(0).getBroadcast();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    String name=MyAddr.getHostName();
                    byte[] data=name.getBytes();

                    if(!MyAddr.isLoopbackAddress()){
                        for(int i=0;i<100;++i){
                            try {
                                ActivityBroadCastSoc.send(
                                        new DatagramPacket(
                                                data,data.length,
                                                BroadcastAddr,
                                                ACTIVITY_SCANNER_PORT_NOT_OWN
                                        )
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();

        new Thread(new Runnable() {

            private InetAddress HostAddr;

            private DatagramPacket CommandPack=null;

            @Override
            public void run() {

                try {
                    CommandSoc=new DatagramSocket(CONNECTION_PORT_OWN);
                    CommandSoc.setSoTimeout(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                while (true){

                    while(true){

                        if(!NetworkConnected()) {
                            break;
                        }

                        for(;NetworkConnected();){

                            CommandPack=null;
                            try {
                                CommandSoc.receive(CommandPack=new DatagramPacket(new byte[1024],1024));
                            } catch (Exception e) {
                                //e.printStackTrace();
                                continue;
                            }

                            String msg=new String(CommandPack.getData(),CommandPack.getOffset(),CommandPack.getLength());
                            String rcvd_dev_id=msg.substring(0,msg.indexOf('|')); msg=msg.substring(msg.indexOf('|')+1);
                            String rcvd_dev_name=msg.substring(0,msg.indexOf('|')); msg=msg.substring(msg.indexOf('|')+1);
                            String rcvd_dev_addr=msg.substring(0,msg.indexOf('|')); msg=msg.substring(msg.indexOf('|')+1);
                            String Command=msg;
                            System.out.println(rcvd_dev_id);
                            System.out.println(rcvd_dev_name);
                            System.out.println(rcvd_dev_addr);
                            System.out.println(Command);

                            if(UtilCollections.ReadFile(UtilCollections.mode_data).equals("1")){

                                if(!UtilCollections.verified_devices_data.exists() || !UtilCollections.ReadFile(UtilCollections.verified_devices_data).contains(rcvd_dev_id)) {

                                    Platform.runLater(new Runnable() {
                                        @Override
                                        public void run() {

                                            Alert alert=new Alert(
                                                    Alert.AlertType.CONFIRMATION,
                                                    rcvd_dev_name +"  ("+rcvd_dev_addr+") is trying to access LocalCommand",
                                                    ButtonType.NO,ButtonType.YES
                                            );
                                            alert.setHeaderText("Allow Connection?");

                                            Optional<ButtonType> res=alert.showAndWait();

                                            if(res.isPresent() && res.get()== ButtonType.YES){

                                                //Todo accepted code
                                                UtilCollections.WriteToFile(UtilCollections.verified_devices_data,(rcvd_dev_id+"\n").getBytes(),true);
                                                byte[] accept_data="_111_".getBytes();
                                                try {
                                                    CommandSoc.send(new DatagramPacket(accept_data,accept_data.length,InetAddress.getByName(rcvd_dev_addr),CONNECTION_PORT_NOT_OWN));
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                /*// Todo next Activity code here
                                                RunCommand(Command); // Todo remove after next activity code is done*/

                                            }else{
                                                byte[] denial_data="_000_".getBytes();
                                                try {
                                                    CommandSoc.send(new DatagramPacket(denial_data,denial_data.length,InetAddress.getByName(rcvd_dev_addr),CONNECTION_PORT_NOT_OWN));
                                                    //continue;
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                    //continue;
                                                }
                                            }
                                        }
                                    });
                                }
                                else {
                                    RunWindowsCommand(Command);
                                    byte[] denial_data="_111_".getBytes();
                                    try {
                                        CommandSoc.send(new DatagramPacket(denial_data,denial_data.length,InetAddress.getByName(rcvd_dev_addr),CONNECTION_PORT_NOT_OWN));
                                        //continue;
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        //continue;
                                    }
                                }

                            } else {
                                RunWindowsCommand(Command);
                                byte[] accept_data="_111_".getBytes();
                                try {
                                    CommandSoc.send(new DatagramPacket(accept_data,accept_data.length,InetAddress.getByName(rcvd_dev_addr),CONNECTION_PORT_NOT_OWN));
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            }

            private boolean NetworkConnected(){
                boolean connected=false;
                try {
                    HostAddr=InetAddress.getLocalHost();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                if(!HostAddr.isLoopbackAddress()){
                    connected=true;
                }
                return connected;
            }

        }).start();

    }

    private void InitializeAppData() {
        try {
            UtilCollections.Install_Dir = new File(UtilCollections.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            UtilCollections.mode_data = new File(UtilCollections.Install_Dir.getAbsolutePath()+ File.separator+"mode.data");
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            UtilCollections.verified_devices_data =
                    new File(
                            UtilCollections.Install_Dir.getAbsolutePath()+ File.separator+
                                    "verified_device_key"
                    );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void RunWindowsCommand(String Command) {
        if(!Command.contains("\n")){
            Command+='\n';
            try {
                Runtime.getRuntime().exec(Command);
            } catch (Exception e) {
                //e.printStackTrace();
            }
        } else {
            if(Command.lastIndexOf('\n')!=Command.length()-1){
                Command+='\n';
            }
            File batch=null;
            try {
                batch=File.createTempFile(Integer.toString((int)(Math.random()*181792)),".bat");
            } catch (IOException e) {
                e.printStackTrace();
            }
            UtilCollections.WriteToFile(batch,Command.getBytes(),false);
            try {
                Runtime.getRuntime().exec("explorer.exe "+"\""+batch.getAbsolutePath()+"\"");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
