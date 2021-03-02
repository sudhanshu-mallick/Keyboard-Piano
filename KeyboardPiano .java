import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;

public class KeyboardPiano {
	// Driver function
	public static void main(String[] args) {
		new KeyboardPiano("q2we4r5ty7u8i9op-[=zxdcfvgbnjmk,.;/", "A");
	}

	private class Key {
		private final String name;
		private final boolean isBlack;
		private final char keyStroke;
		private final double xmin, xmax, ymin, ymax;

		public Key(double x, String name, char keyStroke, boolean isBlack) {
			this.name = name;
			this.keyStroke = keyStroke;
			this.isBlack = isBlack;

			if (!isBlack) {
				xmin = x;
				xmax = x + 1;
				ymin = 0.0;
				ymax = 1.0;
			} else {
				xmin = x - 0.3;
				xmax = x + 0.3;
				ymin = 0.0;
				ymax = 0.6;
			}
		}

		// Draw the key using the given background and foreground colors
		private void draw(Graphics2D g, double width, double height, Color backgroundColor, Color foregroundColor) {
			double SCALE_X = width / whiteKeys.size();
			double SCALE_Y = height;
			Rectangle2D.Double rectangle = new Rectangle2D.Double(xmin * SCALE_X, ymin * SCALE_Y,
					(xmax - xmin) * SCALE_X, (ymax - ymin) * SCALE_Y);

			// Black key
			if (isBlack) {
				g.setColor(backgroundColor);
				g.fill(rectangle);
				g.setFont(getFont(18, width, height));

				FontMetrics metrics = g.getFontMetrics();
				int ws = metrics.stringWidth(keyStroke + "");

				g.setColor(foregroundColor);
				g.drawString(keyStroke + "", (float) ((xmin + xmax) / 2.0 * SCALE_X - ws / 2.0), 25.0f);
			}

			// White key
			else {
				g.setColor(backgroundColor);
				g.fill(rectangle);

				// include outline (since fill color might be white)
				g.setColor(Color.BLACK);
				g.draw(rectangle);
				g.setFont(getFont(18, width, height));

				FontMetrics metrics = g.getFontMetrics();
				int hs = metrics.getHeight();
				int ws = metrics.stringWidth(name);

				g.setColor(foregroundColor);
				g.drawString(keyStroke + "", (float) ((xmin + xmax) / 2.0 * SCALE_X - ws / 2.0),
						(float) (0.95 * SCALE_Y - hs / 2.0));
			}
		}

		// Keyboard keystroke corresponding to this piano key
		private char getKeyStroke() {
			return keyStroke;
		}

		// Check if current cursor position (x,y) is contained in any component area
		private boolean contains(double x, double y) {
			return x >= xmin && x < xmax && y >= ymin && y < ymax;
		}
	}

	// Key colors
	private static boolean WHITE_KEY = false;
	private static boolean BLACK_KEY = true;

	// Frame width and height
	private int fWidth;
	private int fHeight;

	// List containing black and white keys
	private LinkedList<Key> blackKeys = new LinkedList<Key>();
	private LinkedList<Key> whiteKeys = new LinkedList<Key>();

	// For synchronization
	private final Object mouseLock = new Object();
	private final Object keyLock = new Object();

	// Set of keys currently pressed
	private HashSet<Character> keysDown = new HashSet<Character>();

	// Points to the current pointer of mouse clicked key
	private Key mouseKey = null;

	// Constructor
	public KeyboardPiano(String keyboardString, String firstWhiteKey) {
		String[] whiteKeyNames = { "A", "B", "C", "D", "E", "F", "G" };

		// Create the white and black keys
		for (int i = 0; i < keyboardString.length(); i++) {

			// Next key is white
			String whiteKeyName = whiteKeyNames[(whiteKeys.size()) % 7];
			Key whiteKey = new Key(whiteKeys.size(), whiteKeyName, keyboardString.charAt(i), WHITE_KEY);

			whiteKeys.add(whiteKey);

			// Next key is black (black keys occur immediately after A, C, D, F, and G)
			if ("ACDFG".contains(whiteKeyName)) {
				i++;

				if (i >= keyboardString.length())
					break;

				String blackKeyName = whiteKeyName;
				Key blackKey = new Key(whiteKeys.size(), blackKeyName, keyboardString.charAt(i), BLACK_KEY);

				blackKeys.add(blackKey);
			}
		}

		// Setting dimensions for the GUI frame
		fWidth = 60 * whiteKeys.size();
		fHeight = 200;

		// Create and show the GUI
		SwingUtilities.invokeLater(() -> {
			JPanel panel = new KeyboardPanel();

			panel.setPreferredSize(new Dimension(fWidth, fHeight));

			JFrame frame = new JFrame("KEYBOARD PIANO - Sudhanshu Shekhar Mallick (2018UIT2552)");

			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			frame.add(panel);
			frame.pack();
			frame.setLocationRelativeTo(null);
			frame.setVisible(true);
		});
	}

	// JPanel for drawing the keyboard
	private class KeyboardPanel extends JPanel implements MouseListener, KeyListener {
		public KeyboardPanel() {
			addMouseListener(this);
			addKeyListener(this);
			setFocusable(true);
		}

		// Draw the keyboard
		public void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);

			Graphics2D g = (Graphics2D) graphics;
			Dimension size = getSize();
			double width = size.getWidth();
			double height = size.getHeight();

			// Draw the white keys
			for (Key whiteKey : whiteKeys) {

				// mouse click or key typed
				if ((whiteKey == mouseKey) || keysDown.contains(whiteKey.getKeyStroke())) {
					whiteKey.draw(g, width, height, Color.YELLOW, Color.BLACK);
				} else {
					whiteKey.draw(g, width, height, Color.WHITE, Color.BLACK);
				}
			}

			// Draw the black keys
			for (Key blackKey : blackKeys) {

				// mouse click or key typed
				if ((blackKey == mouseKey) || keysDown.contains(blackKey.getKeyStroke())) {
					blackKey.draw(g, width, height, Color.ORANGE, Color.BLACK);
				}

				// draw as usual
				else {
					blackKey.draw(g, width, height, Color.BLACK, Color.WHITE);
				}
			}
		}

		// Mouse cursor events
		public void mouseClicked(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			synchronized (mouseLock) {
				Dimension size = getSize();
				double width = size.getWidth();
				double height = size.getHeight();
				double mouseX = e.getX() / width * whiteKeys.size();
				double mouseY = e.getY() / height;

				// Check black keys first
				for (Key blackKey : blackKeys) {
					if (blackKey.contains(mouseX, mouseY)) {
						mouseKey = blackKey;
						char c = blackKey.getKeyStroke();

						playMusic(c);
						repaint();
						return;
					}
				}

				// next, check white keys
				for (Key whiteKey : whiteKeys) {
					if (whiteKey.contains(mouseX, mouseY)) {
						mouseKey = whiteKey;
						char c = whiteKey.getKeyStroke();

						playMusic(c);
						repaint();
						return;
					}
				}
			}
		}

		public void mouseReleased(MouseEvent e) {
			synchronized (mouseLock) {
				mouseKey = null;

				repaint();
			}
		}

		// Keyboard events
		public void keyTyped(KeyEvent e) {
			synchronized (keyLock) {
			}
		}

		public void keyPressed(KeyEvent e) {
			synchronized (keyLock) {
				char c = Character.toLowerCase(e.getKeyChar());

				playMusic(c);
				keysDown.add(c);
				repaint();
			}
		}

		public void keyReleased(KeyEvent e) {
			synchronized (keyLock) {
				char c = Character.toLowerCase(e.getKeyChar());

				keysDown.remove(c);
				repaint();
			}
		}
	}

	// Styling the keys
	private Font getFont(int defaultFontSize, double width, double height) {
		int size = (int) (width * defaultFontSize / fWidth);

		return new Font("Dialog", Font.BOLD, size);
	}

	void playMusic(char c) {
		if (c == 'q')
			chord("A");
		else if (c == '2')
			chord("B");
		else if (c == 'w')
			chord("C");
		else if (c == 'e')
			chord("D");
		else if (c == '4')
			chord("E");
		else if (c == 'r')
			chord("F");
		else if (c == '5')
			chord("G");
		else if (c == 't')
			chord("A_Drum");
		else if (c == 'y')
			chord("B_Drum");
		else if (c == '7')
			chord("C_Drum");
		else if (c == 'u')
			chord("D_Drum");
		else if (c == '8')
			chord("E_Drum");
		else if (c == 'i')
			chord("F_Drum");
		else if (c == '9')
			chord("G_Drum");
		else if (c == 'o')
			chord("Bb");
		else if (c == 'p')
			chord("C_s");
		else if (c == '-')
			chord("D_s");
		else if (c == '[')
			chord("E1");
		else if (c == '=')
			chord("F_s");
		else if (c == 'z')
			chord("G_s");
		else if (c == 'x')
			chord("Bb_Drum");
		else if (c == 'd')
			chord("C_s1");
		else if (c == 'c')
			chord("D_s1");
		else if (c == 'f')
			chord("E1_Drum");
		else if (c == 'v')
			chord("F1");
		else if (c == 'g')
			chord("Gq_Drum");
		else if (c == 'b')
			chord("C1");
		else if (c == 'n')
			chord("D1");
		else if (c == 'j')
			chord("F1_Drum");
		else if (c == 'm')
			chord("C1_Drum");
		else if (c == 'k')
			chord("D1_Drum");
		else if (c == ',')
			chord("F1_Drum");
		else if (c == '.')
			chord("Cq_Drum");
		else if (c == ';')
			chord("Dq_Drum");
		else if (c == '/')
			chord("Fq_Drum");
	}

	void chord(String name) {
		AudioInputStream audioInputStream;

		try {
			audioInputStream = AudioSystem.getAudioInputStream(new File("E:\\Projects\\MusicNotes\\" + name + ".wav"));
			Clip clip = AudioSystem.getClip();

			clip.open(audioInputStream);
			clip.start();
		} catch (UnsupportedAudioFileException | IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (LineUnavailableException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}