package ro.racai.robin.dialog.generators;

import java.time.LocalDateTime;
import ro.racai.robin.dialog.RDResponseGenerator;

/**
 * Will give the current time to Pepper.
 */
public class TimeNow implements RDResponseGenerator {

    @Override
    public String generate() {
        int hour = LocalDateTime.now().getHour();
        int min = LocalDateTime.now().getMinute();

        return hour + ":" + min;
    }
}
