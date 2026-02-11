package dslabs.atmostonce;

import dslabs.framework.Address;
import dslabs.framework.Application;
import dslabs.framework.Command;
import dslabs.framework.Result;
import java.util.HashMap;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode
@ToString
@RequiredArgsConstructor
public final class AMOApplication<T extends Application> implements Application {
  @Getter @NonNull private final T application;

  // Your code here...
  private final HashMap<Address, Integer> clientSeq = new HashMap<>();
  private final HashMap<Address, Result> clientResults = new HashMap<>();

  @Override
  public AMOResult execute(Command command) {
    if (!(command instanceof AMOCommand)) {
      throw new IllegalArgumentException();
    }

    AMOCommand amoCommand = (AMOCommand) command;

    // Your code here...
    Address clientAddress = amoCommand.address();
    Integer seq = amoCommand.sequence();
    Integer clientSequence = clientSeq.getOrDefault(clientAddress, 0);
    //There are three kind of situation:
    // First is seq == clientSeq, this means this command need to be executed;
    // Second is seq + 1 == clientSeq, this means this command have been executed and already
    // have a result;
    // Third is seq + 1 < clientSeq, this means this command is out of date and should be dropped.
    if (seq.equals(clientSequence)) {
      Command appCommand = amoCommand.command();
      Result result = application.execute(appCommand);
      clientSeq.put(clientAddress, clientSequence + 1);
      clientResults.put(clientAddress, result);
      return new AMOResult(result, seq);
    } else if (seq.equals(clientSequence - 1)) {
      Result result = clientResults.get(clientAddress);
      return new AMOResult(result, seq);
    } else {
      return null;
    }
  }

  public Result executeReadOnly(Command command) {
    if (!command.readOnly()) {
      throw new IllegalArgumentException();
    }

    if (command instanceof AMOCommand) {
      return execute(command);
    }

    return application.execute(command);
  }

  public boolean alreadyExecuted(AMOCommand amoCommand) {
    // Your code here...
    return false;
  }
}
