package com.silicolife.wrappergate.tagger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
/**
 * This class contain method for manager directory and files
 * 
 * 
 */
public class FileHandling {
	
	public static boolean createDirectory(String dir){
		
		return (new File(dir)).mkdir();
	
	}
	
	public static boolean existDirectory(String dir){
	
		return (new File(dir)).exists();
		
	}
	
	/**
	 * Method that remove a directory
	 * 
	 * @param dir - Directory File
	 * @return true -- If process completes
	 * 		   false -- otherwise
	 */
	public static boolean removeDirectory(File dir){
		//If the directory is not empty, it is necessary to first recursively delete all files and subdirectories in the directory. Here is a method that will delete a non-empty directory.
		
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for(String child: children)
			{
				boolean success = removeDirectory(new File(dir, child));
				if (!success) {
					// Deletion failed
					System.exit(0);
					return false;					
				}
			}
			return true;
		}
			
		// The directory is now empty so delete it
		return removeEmptyDirectory(dir);
	
	}
	
	private static boolean removeEmptyDirectory(File dir){		
		return dir.delete();
	}
	
	/**
	 * Method that copy a file
	 * 
	 * @param in - File input
	 * @param out - file output
	 * @return true -- If process completes
	 * 		   false -- otherwise
	 */
	public static boolean copyFile(File in, File out){
		try {
			FileReader fr = new FileReader(in);
			FileWriter fw = new FileWriter(out);
			BufferedReader br = new BufferedReader(fr);
			BufferedWriter bw = new BufferedWriter(fw);
			
			String line = br.readLine();
			while(line != null)
			{
				bw.write(line + "\n");
				line = br.readLine();
			}
			
			br.close();
			bw.close();
			fr.close();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * Method that copy a file in Binary
	 * 
	 * @param in - File input
	 * @param out - file output
	 * @return true -- If process completes
	 * 		   false -- otherwise
	 */
	public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);
    
        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
	
	public static boolean appendToFile(String text, File in, File out){
		try {
			FileReader fr = new FileReader(in);
			FileWriter fw = new FileWriter(out);
			BufferedReader br = new BufferedReader(fr);
			BufferedWriter bw = new BufferedWriter(fw);
			
			String line = text;
			while(line != null)
			{
				bw.write(line + "\n");
				line = br.readLine();
			}
			
			br.close();
			bw.close();
			fr.close();
			fw.close();
 		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 *  Method that return a list with names of the files is a given directory
	 *  
	 *  @param directory - Directory
	 *  @return list with names of the files on directory
	 */
	public static ArrayList<String> listFileNames(File directory){
		ArrayList<String> fileList = new ArrayList<String>();
		
		if(!directory.isDirectory())
			return fileList;
		
		for(File file: directory.listFiles())
			fileList.add(file.getName());
		
		return fileList;
	}
	
}
