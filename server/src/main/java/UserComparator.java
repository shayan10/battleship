import java.util.Comparator;

public class UserComparator implements Comparator<User> {
    public int compare(User user1, User user2) {
        double userOneRanking = user1.getEloRanking();
        double userTwoRanking = user2.getEloRanking();
        if (userOneRanking < userTwoRanking) {
            return 1;
        } else if (userOneRanking > userTwoRanking) {
            return -1;
        } else {
            return 0;
        }
    }
}