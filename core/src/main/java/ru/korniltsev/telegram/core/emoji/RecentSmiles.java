package ru.korniltsev.telegram.core.emoji;

import android.content.Context;
import android.content.SharedPreferences;
import com.crashlytics.android.core.CrashlyticsCore;
import ru.korniltsev.telegram.core.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecentSmiles {
    static final String PREF_RECENT_SMILES = "pref_recent_smiles";
    public static final String DIVIDER_ENTRY = ";";
    public static final String DIVIDER_TIME_CODE = ",";

    final Set<Entry> recent = new HashSet<>();
    private final SharedPreferences prefs;
    final int max;
    public RecentSmiles(Context ctx, String name, int max) {
        this.max = max;
        this.prefs = ctx.getSharedPreferences(name, Context.MODE_PRIVATE);
        String string = this.prefs.getString(PREF_RECENT_SMILES, "");
        String[] split = string.split(DIVIDER_ENTRY);
        try {
            for (int i = 0; i < split.length; i++) {
                String s = split[i];
                if (s.length() == 0) {
                    continue;
                }
                String[] timeCode = s.split(DIVIDER_TIME_CODE);

                long time = Long.parseLong(timeCode[0]);
                String code = timeCode[1];
                recent.add(new Entry(time, code));
            }
        } catch (Exception e) {
            CrashlyticsCore.getInstance().logException(e);
        }
    }

    public void count(String emojiCode) {
        Entry e = new Entry(System.currentTimeMillis(), emojiCode);
        recent.remove(e);
        recent.add(e);
        saveToPrefs();
    }

    public List<Entry> getRecent() {
        ArrayList<Entry> res = new ArrayList<>(recent);
        Collections.sort(res, new Comparator<Entry>() {
            @Override
            public int compare(Entry lhs, Entry rhs) {
                return Utils.compare(rhs.time, lhs.time);
            }
        });

        return res;
    }


    private void saveToPrefs() {
        List<Entry> es = getRecent();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < es.size(); i++) {
            Entry e = es.get(i);
            sb.append(e.time)
                    .append(DIVIDER_TIME_CODE)
                    .append(e.code);
            if (i != es.size() - 1) {
                sb.append(DIVIDER_ENTRY);
            }
        }
        prefs.edit()
                .putString(PREF_RECENT_SMILES, sb.toString())
                .apply();
    }

    class Entry {
        final long time;
        final String code;

        public Entry(long time, String code) {
            this.time = time;
            this.code = code;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Entry entry = (Entry) o;

            return code.equals(entry.code);
        }

        @Override
        public int hashCode() {
            return code.hashCode();
        }
    }
}
