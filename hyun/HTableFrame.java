package hyun;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

@SuppressWarnings("serial")
public class HTableFrame extends JFrame implements ActionListener, KeyListener {

	private Object[][] cells = new Object[][]{{"","","",""}}; 
	private String[] colNames = new String[]{"이름","나이","전화번호","성별"};
	private JTable table;
	private DefaultTableModel model;
	
	private JFrame frame;
	private JMenuBar menuBar;
	private JMenuItem save;
	private JMenuItem load;
	private FileDialog fd;
	private TableOutput to;
	private TableInput ti;
	
	public HTableFrame()
	{
		setTitle("PhoneBook"); 
        setSize(300,300);
        
        //메뉴바
        menuBar = new JMenuBar();
        JMenu menu = new JMenu("파일");
		save = new JMenuItem("저장"); 
		menu.add(save);
		menuBar.add(menu);
		load = new JMenuItem("열기");
		menu.add(load);
		menuBar.add(menu);
		this.setJMenuBar(menuBar);
		
		//메뉴바 이벤트
		save.setActionCommand("save");
		save.addActionListener(this);
		
		load.setActionCommand("load");
		load.addActionListener(this);
        
		//테이블
        model = new DefaultTableModel(cells,colNames);
        table = new JTable();
        table.setModel(model);
        //table.addMouseListener(this);
        table.addKeyListener(this);
    
        JScrollPane scrollPane = new JScrollPane(table); 
        Container contentPane = this.getContentPane();
        contentPane.add(scrollPane,BorderLayout.CENTER);
	}
	
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		Object cmd = e.getActionCommand();
		
		if(cmd.equals("save"))
		{
			fd = new FileDialog(frame, "저장", FileDialog.SAVE);
			fd.setVisible(true);
			if(fd.getFile() != null)
			{
				
				to = new TableOutput(fd.getDirectory()+"\\"+fd.getFile());
				to.saveTable(model);
			}
		}
		else if(cmd.equals("load"))
		{
			fd = new FileDialog(frame, "열기", FileDialog.LOAD);
			fd.setVisible(true);
			
			if(fd.getFile() != null)
			{	
				ti = new TableInput(fd.getDirectory()+"\\"+fd.getFile());
				table.setModel(ti.loadTable(table,model));
				//table.addMouseListener(this);
				table.addKeyListener(this);
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Auto-generated method stub
		int sRow = table.getSelectedRow();
		int sCol = table.getSelectedColumn();
		int tRow = table.getRowCount();
		int tCol = table.getColumnCount();
		
		if(sRow+1 == tRow && sCol+1 == tCol)
		{
			boolean f = true;
			for(int i = 0; i < tCol-1; i++)
			{
				if(table.getValueAt(sRow, i) == "")
				{
					f = false;
				}
			}
			
			if(f)
			{
				model.addRow(new Object[]{"","","",""});
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
}
