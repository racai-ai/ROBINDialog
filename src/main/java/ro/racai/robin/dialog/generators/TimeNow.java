package ro.racai.robin.dialog.generators;

import java.util.Calendar;
import ro.racai.robin.dialog.RDResponseGenerator;

/**
 * Will give the current time to Pepper, the robot.
 */
public class TimeNow implements RDResponseGenerator {

    @Override
    public String generate() {
        Calendar rightNow = Calendar.getInstance();
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        int min = rightNow.get(Calendar.MINUTE);

        return hour + ":" + min;
    }
}
