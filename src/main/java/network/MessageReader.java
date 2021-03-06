/*
 * Gomoku
 * Maciej Kawecki 2015/16
 */
package network;

import java.awt.Color;
import java.io.IOException;
import java.net.SocketException;


/**
 *
 * Wątek odbierający wiadomości dla lokalnego gracza - klienta
 * 
 * @author Maciej Kawecki
 * 
 */
public class MessageReader extends Thread {
    
  /** Referencja do obiektu klienta */
  private final Client client;
  
  
  /**
   * Konstruktor 
   * @param client Referencja do obiektu klienta
   */
  public MessageReader(Client client) {
      
    this.client = client; 
    setDaemon(true);
      
  }
  
  
  @Override
  public void run() {   
      
   try {
          
     // pobranie i wyświetlenie wiadomości
     Command cmd = client.getResponse();
     
     // ping - koniec kolejki
     if (cmd.getCommand() == Command.CMD_PING) return;
     
     if (cmd.getCommand()!=Command.CMD_MESSAGE) throw new IOException();  
     client.getConsole().newLine();
     client.getConsole().setMessageLn((String)(cmd.getCommandData()), new Color(0x22, 0x8b, 0x22));
            
   } 
     
   // przerwanie odczytywania z wątku klienta - nic nie robić
   catch (SocketException e) {}
   
   catch (IOException e) {}

   
  }
  
  

}
