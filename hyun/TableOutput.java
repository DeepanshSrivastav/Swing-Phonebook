package hyun;

import java.io.*;
import javax.swing.table.TableModel;

public class TableOutput {
	
	private File file;
	private FileWriter fw;
	
	public TableOutput(String filepath) 
	{
		try{
			
		// TODO Auto-generated constructor stub
		file = new File(filepath);
		
		fw = new FileWriter(file);
		
		}catch(IOException e){
			
		}
	}
	
	public void saveTable(TableModel model)
	{
		try{
			
			fw.write(model.getRowCount());
			fw.write(model.getColumnCount());
			
			for(int i = 0; i < model.getRowCount(); i++)
			{
				for(int j = 0; j < model.getColumnCount(); j++)
				{
					if(model.getValueAt(i, j).toString() == "")
					{
						fw.write("\r");
					}
					else
					{
						fw.write(model.getValueAt(i, j).toString());
					}
					fw.write("\n");
					fw.flush();
				}
			}
			fw.write(0);
			
			fw.flush();
			fw.close();
				
		}catch(IOException e){
			
		}	 
	}
}
