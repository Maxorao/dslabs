package dslabs.clientserver;

import dslabs.atmostonce.AMOCommand;
import dslabs.atmostonce.AMOResult;
import dslabs.framework.Address;
import dslabs.framework.Client;
import dslabs.framework.Command;
import dslabs.framework.Node;
import dslabs.framework.Result;
import java.util.Objects;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Simple client that sends requests to a single server and returns responses.
 *
 * <p>See the documentation of {@link Client} and {@link Node} for important implementation notes.
 */
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class SimpleClient extends Node implements Client {
  private final Address serverAddress;

  // Your code here...
  private Result currentResult = null;
  private Integer sequence = 0;
  private Command currentCommand = null;

  /* -----------------------------------------------------------------------------------------------
   *  Construction and Initialization
   * ---------------------------------------------------------------------------------------------*/
  public SimpleClient(Address address, Address serverAddress) {
    super(address);
    this.serverAddress = serverAddress;
  }

  @Override
  public synchronized void init() {
    // No initialization necessary
  }

  /* -----------------------------------------------------------------------------------------------
   *  Client Methods
   * ---------------------------------------------------------------------------------------------*/
  @Override
  public synchronized void sendCommand(Command command) {
    // Your code here...
    currentCommand = command;
    currentResult = null;
    AMOCommand amoCommand = new AMOCommand(command, super.address(), sequence);
    Request request = new Request(amoCommand);
    send(request, serverAddress);
    ClientTimer clientTimer = new ClientTimer(sequence);
    set(clientTimer, ClientTimer.CLIENT_RETRY_MILLIS);
  }

  @Override
  public synchronized boolean hasResult() {
    // Your code here...
    return currentResult != null;
  }

  @Override
  public synchronized Result getResult() throws InterruptedException {
    // Your code here...
    while (currentResult == null) {
      wait();
    }
    return currentResult;
  }

  /* -----------------------------------------------------------------------------------------------
   *  Message Handlers
   * ---------------------------------------------------------------------------------------------*/
  private synchronized void handleReply(Reply m, Address sender) {
    // Your code here...
    if (!sender.equals(serverAddress)) {
      return;
    }

    AMOResult amoResult = m.amoResult();
    Integer resultSeq = amoResult.sequence();

    if (Objects.equals(resultSeq, sequence)) {
      sequence++;
      currentResult = m.amoResult().result();
      currentCommand = null;
      notify();
    }
  }

  /* -----------------------------------------------------------------------------------------------
   *  Timer Handlers
   * ---------------------------------------------------------------------------------------------*/
  private synchronized void onClientTimer(ClientTimer t) {
    // Your code here...
    Integer timeSequence = t.sequence();
    if (Objects.equals(timeSequence, sequence) && currentCommand != null) {
      sendCommand(currentCommand);
    }
  }
}
