package playground;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Count {
	
	public static final int _FILE_CNT_LMT = 10;

	public static void main(String[] args) throws Exception {
		
		
        System.out.println("===== START =====");
		
        Files.walk(Paths.get("F:\\Sharuru")).forEach(p -> {
            if(p.toFile().isDirectory()){
                if(p.toFile().list().length > _FILE_CNT_LMT){
                    System.out.println(p.toAbsolutePath().toString() + " ==> " + p.toFile().list().length);
                }
            }
        });
        
        System.out.println("===== END =====");

	}

}
