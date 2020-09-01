package io.suptest;

import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class Display extends JFrame {

	private PPU ppu;
	
	class DisplayComponent extends JComponent {
		private static final long serialVersionUID = 1L;

		public void paint(Graphics g) {
			g.drawImage(ppu.getBackBuffer(), 0, 0, null);
		}
		
	}
	
	public Display(PPU ppu) {
		this.ppu = ppu;
		setSize(800, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		add(new DisplayComponent());
		
		JMenuBar bar = new JMenuBar();
		{
			JMenu menu = new JMenu("File");
			{
				JMenuItem item = new JMenuItem("Open..");
				item.addActionListener((event) -> {
					// TODO: open ROM
				});
				menu.add(item);
			}
			bar.add(menu);
		}
		setJMenuBar(bar);
		setVisible(true);
	}
	
}
