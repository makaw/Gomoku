/*
 * Gomoku
 * Maciej Kawecki 2015/16
 */
package gui;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import java.util.Observable;
import java.util.Observer;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.OceanTheme;

import game.GameMode;
import game.GameState;
import gomoku.AppObserver;
import gomoku.IConf;
import gomoku.Lang;
import gomoku.Settings;
import gui.dialogs.ConfirmDialog;
import gui.dialogs.PromptDialog;

/**
 *
 * Szablon obiektu budującego graficzny interfejs użytkownika
 * 
 * @author Maciej Kawecki
 * 
 */
@SuppressWarnings("serial")
public class GUI extends JFrame implements IBaseGUI, Observer {
  
  
  /** Szerokość okna aplikacji w pikselach */
  protected static final int F_WIDTH = 395;
  /** Wysokość okna aplikacji w pikselach */
  protected static final int F_HEIGHT = 558;       
  
  /** Graficzna reprezentacja planszy */  
  private BoardGraphics board;
  /** Konsola */
  private final Console console;
  /** Wybrany tryb gry */
  private GameMode gameMode;
  /** Obiekt służący do odtwarzania dźwięków */  
  private Sounds sounds;
  /** Obserwator do komunikacji z wątkiem kontrolującym przebieg gry  */
  private final AppObserver gameSpy;
  /** Obiekt przechowujący ustawienia gry */
  private final Settings settings;
  /** Panel w którym zawiera się graficzna reprezentacja planszy */
  private final JPanel panelBoard;
  /** Obiekt przycisku do wysyłania wiadomości w grze sieciowej */
  private final JButton msgButton;
  /** Przycisk rozłączenia z serwerem */
  private final JButton dscButton;
  /** Pasek statusu */
  private final StatusBar statusBar;
  /** Menu gra */
  private final MenuGame menuGame;
  /** Menu pomoc */
  private final MenuHelp menuHelp;
  /** Gniazdko dla klienta */
  private Socket socket;  
   
   private final JPanel csPanel;
  
  /**
   * Konstruktor
   * @param gameSpy Obserwator - komunikacja z wątkiem kontrolującym przebieg gry
   */  
  public GUI(final AppObserver gameSpy, Settings settings) {
      
    super("Gomoku");  
    
    this.gameSpy = gameSpy;    
    this.settings = settings;
    
    setIconImage(ImageRes.getImage("icon_small.png"));  
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    
    // umieszczenie okna programu na środku ekranu
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    setBounds((int)((screenSize.getWidth() - F_WIDTH)/2), 
              (int)((screenSize.getHeight() - F_HEIGHT)/2), F_WIDTH, F_HEIGHT);
      
    getContentPane().setLayout(new BorderLayout());

    board = new BoardGraphics(settings.getColsAndRows());
    sounds = new Sounds();

    // przygotowanie dolnej części ekranu - konsola i przyciski
    JPanel panelConsole = new JPanel();
    GridBagLayout gb = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    panelConsole.setLayout(gb);

    
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.RELATIVE;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
  
    
    JPanel panelButtons = new JPanel(new GridLayout(3,1));
  
    // przycisk do wysyłania wiadomości
    msgButton = new JButton("", ImageRes.getIcon("message.png"));
    msgButton.setFocusPainted(false);
    msgButton.setToolTipText(Lang.get("SendMessage"));
    msgButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        String msg = new PromptDialog(GUI.this, Lang.get("MessageContent") + ":").getAnswer();
        if (msg!=null && !msg.isEmpty()) {
          gameSpy.sendObject("message", msg);
        } 
        
       }
    });   
    // domyślnie zablokowany, ma być aktywny tylko podczas gry sieciowej, włączanie w interfejsie Console
    msgButton.setEnabled(false);
     
    // przycisk do włączania/wyłączania dźwięku
    final JButton sndButton = new JButton("", ImageRes.getIcon(IConf.DEFAULT_ENABLE_SOUND ? "sound.png" : "mute.png"));
    sndButton.setFocusPainted(false);
    sndButton.setToolTipText(Lang.get("SoundOnOff"));
    sndButton.addActionListener(new ActionListener() {
       @Override
       public void actionPerformed(final ActionEvent e) {
         sndButton.setIcon(sounds.isEnabled() ? ImageRes.getIcon("mute.png") : ImageRes.getIcon("sound.png"));
         sounds.toggleSound();
       }
    });        
    
    
    menuGame = new MenuGame(this);
    menuHelp = new MenuHelp(this);
    
    ActionListener dscList = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
              
         boolean res = new ConfirmDialog(GUI.this, Lang.get("DisconnectConfirm")).isConfirmed(); 
            
         if (res)  try {
           socket.close();
           dscButton.setEnabled(false);
           menuGame.enableItems(true);
         } 
         catch (IOException ex) {
           System.err.println(ex);  
         }          
            
      }
         
    };
    

    menuGame.getDscItem().addActionListener(dscList);            
    
    // przycisk rozłączenia z serwerem
    dscButton = new JButton("", ImageRes.getIcon("disconnect.png"));
    dscButton.setFocusPainted(false);
    dscButton.setToolTipText(Lang.get("DisconnectServer"));
    dscButton.addActionListener(dscList);
    dscButton.setEnabled(false);
    
    panelButtons.add(sndButton);
    panelButtons.add(msgButton);
    panelButtons.add(dscButton);

    panelButtons.setPreferredSize(new Dimension(40, 104));    
    gb.setConstraints(panelButtons, gbc);
    panelConsole.add(panelButtons);
     
    gbc.weighty = 1.0;
    gbc.weightx = 2.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.gridheight = 1;
    
 
    console = new Console(sndButton, msgButton, dscButton, menuGame);
    console.setPreferredSize(new Dimension(F_WIDTH-70, 102));
    console.setMaximumSize(new Dimension(F_WIDTH-70, 102));
    console.setSize(new Dimension(F_WIDTH-70, 102));
    
    gb.setConstraints(console, gbc);
    panelConsole.add(console);
     
    panelConsole.add(new JScrollPane(console, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                     JScrollPane.HORIZONTAL_SCROLLBAR_NEVER));
    
    
    // zagnieżdżenie graficznej planszy w panelu
    panelBoard = new JPanel(new GridLayout(1,1));
    panelBoard.add(board);

    csPanel = new JPanel();
    csPanel.setLayout(new BoxLayout(csPanel, BoxLayout.Y_AXIS));
    
    statusBar = new StatusBar(this); 
    csPanel.add(statusBar);    
    csPanel.add(panelConsole);
    
    getContentPane().add(panelBoard, BorderLayout.NORTH);
    getContentPane().add(csPanel, BorderLayout.SOUTH);

    // stworzenie glownego menu aplikacji
    JMenuBar menu = new JMenuBar();
    menu.add(menuGame);
    menu.add(menuHelp);
    setJMenuBar(menu);    
    
    pack();
    setSize(F_WIDTH, F_HEIGHT);
    setResizable(false);
    setVisible(true);

    // witamy sie :-)
    console.setMessageLn(Lang.get("Hello"), Color.BLACK); 
    
    gameSpy.addObserver(this);    

  } 

  
  /**
   * Metoda obsługująca uruchomienie nowej gry: zmiana stanu (i ew. adresu IP serwera), powiadomienie wątku 
   *  kontrolującego przebieg gry o zmianie, wyczyszczenie graficznej planszy i konsoli
   * @param gameMode Wybrany przez użytkownika tryb nowej gry
   * @param serverIP Adres IP serwera
   * @see game.Game#update(java.util.Observable, java.lang.Object) 
   */
  @Override
  public void restartGame(GameMode gameMode, String serverIP) {      
      
	GameState gs = GameState.RESTART;
	gs.setServerIP(serverIP);  
	  
    if (this.gameMode == GameMode.NETWORK_GAME && socket != null) {
         
      try {
         socket.close();
      } catch (IOException ex) {
         System.err.println(ex);
      }
      
      gameSpy.sendObject("state", gs);
          
    } 
      
      
    this.gameMode = gameMode;
    
    // przesłanie do wątka gry informacji o zmianie stanu    
    gameSpy.sendObject("state", gs);
    board.clear();
    console.clear();   
    
    if (gameMode == GameMode.NETWORK_GAME) 
      console.setMessageLn(Lang.get("NetworkModeChosen"), Color.DARK_GRAY);
              
    else {
      console.setMessageLn(Lang.get("ModeChosen"), Color.GRAY);    
    }
    
    
  }
  
  
  /**
   * Zatrzymanie rozgrywki
   */
  public void cancelGame() {
	  
	  gameSpy.sendObject("state", GameState.WAIT);
	  console.setMessageLn("\n" + Lang.get("GameCancelled"), Color.RED);
	  console.newGameMsg();
	  board.setCursor(null);
	  
  }
  
  
  /**
   * Metoda obsługująca zmianę ustawień przez użytkownika: zatrzymanie bieżącej rozgrywki, 
   * powiadomienie wątku kontrolującego przebieg gry o zmianie stanu, wyczyszczenie konsoli, przygotowanie 
   * nowej graficznej planszy, oraz przesłanie do wątku kontrolującego przebieg gry nowych ustawień i 
   * referencji do nowej graficznej reprezentacji planszy
   * @see game.Game#update(java.util.Observable, java.lang.Object) 
   */
  @Override
  public void restartGameSettings() {

    // przesłanie do wątka gry informacji o zmianie stanu
    gameSpy.sendObject("state", GameState.WAIT);
    
    console.clear();
    console.setMessageLn(Lang.get("GameCancelledSettings"), Color.RED);
    
    board = new BoardGraphics(settings.getColsAndRows());
    panelBoard.removeAll();
    panelBoard.add(board);
    revalidate();
    
    try {
      Thread.sleep(100);  
    }
    catch(InterruptedException e) {}
    
    // przesłanie do wątka gry nowych ustawien i referencji do nowej planszy
    gameSpy.sendObject("settings-main", settings);
    gameSpy.sendObject("board", board);
    
    console.newGameMsg();       

  }  
  
  
 /**
   * Metoda obsługująca zmianę ustawień dla trybu klienta (ustawienia pobrane z serwera):
   * wyczyszczenie konsoli, przygotowanie nowej graficznej planszy,
   * oraz przesłanie do wątku kontrolującego przebieg gry referencji do nowej
   * graficznej reprezentacji planszy
   * @param colsAndRows Ilość wierszy i kolumn dla klienta
   * @see game.Game#update(java.util.Observable, java.lang.Object) 
   */
  public void restartClientGameSettings(int colsAndRows) {
    
    console.clear();
    console.setMessageLn(Lang.get("ConnectedWithSecondPlayer"), new Color(0xa5, 0x2a, 0x2a));
    
    board = new BoardGraphics(colsAndRows);
    panelBoard.removeAll();
    panelBoard.add(board);
    revalidate();
    
    try {
      Thread.sleep(100);  
    }
    catch(InterruptedException e) {}
    
    // przesłanie do wątka gry nowych ustawien i referencji do nowej planszy
    gameSpy.sendObject("settings", settings);
    gameSpy.sendObject("board", board);

  }    

 
  public Console getConsole() {
      
     return console; 
      
  }
  
  
  public StatusBar getStatusBar() {
	  
	 return statusBar; 
	  
  }
  
  
  public BoardGraphics getBoard() {
      
    return board; 
      
  }
  
  public GameMode getGameMode() {
      
    return gameMode; 
      
  }  
  
  public Sounds getSounds() {
      
    return sounds;  
      
  }

  
  @Override
  public Settings getSettings() {
      
     return settings;  
      
  }
  
  
  public MenuGame getMenuGame() {
	 return menuGame;
  }
  
  
  /**
   * Metoda odpowiada: to nie serwer
   * @return false
   */
  @Override
  public boolean isServer() {
      
     return false;  
      
  }
  
  /**
   * Statyczna metoda ustawiająca temat(Ocean) LookAndFeel 
   */
  public static void setLookAndFeel() {
      
    MetalLookAndFeel.setCurrentTheme(new OceanTheme());
    try {
      UIManager.setLookAndFeel(new MetalLookAndFeel());
    }
    catch(Exception e) {
      System.err.println(e);
    }
    
    JFrame.setDefaultLookAndFeelDecorated(true);   
    JDialog.setDefaultLookAndFeelDecorated(true);   
      
  }  

  
  @Override
  public void translate() {
	  	 
	console.clear();
	
	for (Component c: IBaseGUI.getAllLocalised(this)) ((ILocalizable)c).translate();
	
	console.setMessageLn("[" + Lang.get("Language") + ": " + Lang.getName() + "]", Color.GRAY);
	console.setMessageLn(Lang.get("Hello"), Color.BLACK);
	console.newLine();
	
  }
  
  
   /**
   * Metoda ustawia referencje przekazane przez obserwatora
   * @param o Obserwowany obiekt 
   * @param object Przekazany obiekt
   */
   @Override
   public void update(Observable o, Object object) {
      
     AppObserver obs = (AppObserver)object;
     
     // przekazanie referencji do utworzonego gniazdka klienta / informacja
     switch (obs.getKey()) {
         
         case "socket": 
             
           socket = (Socket)obs.getObject();
           dscButton.setEnabled(true);
           menuGame.enableItems(false);
           menuGame.getDscItem().setEnabled(true);
           menuGame.getDscItem().setVisible(true);
           
           break;
          
         case "socket-state":
        	 
           String state = obs.getObject().toString();
           if (state.equals("wait"))
             console.setMessageLn(Lang.get("OKWaitingForSecondPlayer"), Color.DARK_GRAY);
           break;
           
     }
           
     
   }

    
}
  
