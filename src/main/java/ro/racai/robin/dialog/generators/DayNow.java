package ro.racai.robin.dialog.generators;

import java.util.Calendar;
import ro.racai.robin.dialog.RDResponseGenerator;

public class DayNow implements RDResponseGenerator {

    @Override
    public String generate() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        String currentDay = null;

        switch (day) {
            case Calendar.MONDAY:
                currentDay = "luni";
                break;
            case Calendar.TUESDAY:
                currentDay = "marți";
                break;
            case Calendar.WEDNESDAY:
                currentDay = "miercuri";
                break;
            case Calendar.THURSDAY:
                currentDay = "joi";
                break;
            case Calendar.FRIDAY:
                currentDay = "vineri";
                break;
            case Calendar.SATURDAY:
                currentDay = "sâmbătă";
                break;
            case Calendar.SUNDAY:
                currentDay = "duminică";
                break;
            default:
                currentDay = null;
        }

        return currentDay;
    }
}
