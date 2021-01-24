import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
public final class UtilCollections {

    public static File Install_Dir;
    public static File mode_data;
    public static File verified_devices_data;

    public static void WriteToFile(File file,byte[] data,boolean append){
        FileOutputStream outputStream=null;
        try {
            outputStream=new FileOutputStream(file,append);
            outputStream.write(data);
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String ReadFile(File file){
        FileInputStream inputStream=null;
        byte[] data=new byte[((int) file.length())];
        try {
            inputStream=new FileInputStream(file);
            inputStream.read(data);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(data);
    }
}
