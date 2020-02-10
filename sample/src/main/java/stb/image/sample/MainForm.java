package stb.image.sample;

import org.nothings.stb.image.ColorComponents;
import org.nothings.stb.image.ImageResult;
import stb.image.sample.utility.Swing;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;

public class MainForm extends JFrame {
	private static class MyPanel extends JPanel {
		BufferedImage img;

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			if (img != null) {
				g.drawImage(img, 0, 0, this);
			}
		}
	}

	private MyPanel panel;

	private MainForm() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		setSize(1200, 800);

		// Main Menu
		JMenuBar mainMenu = new JMenuBar();

		// File
		JMenu menuFile = new JMenu("File");

		// Open
		JMenuItem menuItemOpen = new JMenuItem("Open");
		menuItemOpen.addActionListener(e -> MainForm.this.onOpen());
		menuFile.add(menuItemOpen);
		mainMenu.add(menuFile);
		setJMenuBar(mainMenu);

		panel = new MyPanel();
		setContentPane(panel);
	}

	private static List<FileFilter> getFileFilters() {
		List<FileFilter> result = new LinkedList<>();

		result.add(new FileNameExtensionFilter("PNG Files(*.png)", "png"));
		result.add(new FileNameExtensionFilter("JPEG Files(*.jpg)", "jpg"));
		result.add(new FileNameExtensionFilter("BMP Files(*.bmp)", "bmp"));
		result.add(new FileNameExtensionFilter("GIF Files(*.gif)", "gif"));
		result.add(new FileNameExtensionFilter("TGA Files(*.tga)", "tga"));
		result.add(new FileNameExtensionFilter("PSD Files(*.psd)", "psd"));

		return result;
	}

	private void onOpen() {
		JFileChooser fc = new JFileChooser();

		fc.setAcceptAllFileFilterUsed(false);
		fc.setCurrentDirectory(new File("D:\\Temp\\"));
		for (FileFilter ff : getFileFilters()) {
			fc.addChoosableFileFilter(ff);
		}

		int dialogResult = fc.showOpenDialog(this);
		if (dialogResult == JFileChooser.APPROVE_OPTION) {
			Open(fc.getSelectedFile().getPath());
		}
	}

	private void Open(String filePath) {
		try {
			byte[] bytes = Files.readAllBytes(new File(filePath).toPath());
			ImageResult image = ImageResult.FromData(bytes, ColorComponents.RedGreenBlueAlpha);
			panel.img = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
			int i = 0;

			short[] data = image.getData();
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int col = new Color(data[i],
							data[i + 1],
							data[i + 2],
							data[i + 3]).getRGB();
					panel.img.setRGB(x, y, col);
					i += 4;
				}
			}

			panel.repaint();
		} catch (Exception ex) {
			Swing.showErrorMessageBox(this, ex.getMessage());
		}
	}

	public static void main(String[] args) {
		MainForm frame = new MainForm();
		Swing.centreWindow(frame);
		frame.setVisible(true);
	}
}