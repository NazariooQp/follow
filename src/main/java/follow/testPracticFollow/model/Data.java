package follow.testPracticFollow.model;

import java.util.UUID;

@lombok.Data
public class Data {
    private final UUID follower;
    private final UUID target;
    private final String playerName;
}
