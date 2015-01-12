package hyun;

import java.io.*;
import java.util.StringTokenizer;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

public class TableInput {
	
	private File file;
	private FileReader fr;
	private int rowCount;
	private int colCount;
	
	public TableInput(String filepath) {
		// TODO Auto-generated constructor stub
		try{
			
			file = new File(filepath);
			
			fr = new FileReader(file);
			
			
		}catch(IOException e){
			
		}
	}
	
	public TableModel loadTable(JTable table, TableModel model)
	{
		try{		
			rowCount = fr.read();
			colCount = fr.read();
			DefaultTableModel model1 = (DefaultTableModel)model;

			StringBuffer strBuf;
			
			strBuf = new StringBuffer();
			
			while(true)
			{	
				int ch = fr.read();
				if(ch == -1)
				{
					break;
				}
				strBuf.append((char)ch);
			}
			
			StringTokenizer st = new StringTokenizer(strBuf.toString(),"\n");
			
			for(int i = 0; i < rowCount; i++)
			{
				if(i != 0)
				{
					model1.addRow(new Object[]{"","","",""});
				}
				
				for(int j = 0; j < colCount; j++)
				{	
					String s = st.nextToken();
					if(s.equals("\r"))
					{
						model.setValueAt("", i, j);
					}
					else
					{
						model.setValueAt(s, i, j);
					}	
				}
			}
			
		}catch(IOException e){
			
		}
		
		return model;
	}
}
