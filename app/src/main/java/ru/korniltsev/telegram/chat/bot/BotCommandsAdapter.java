package ru.korniltsev.telegram.chat.bot;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.drinkless.td.libcore.telegram.TdApi;
import ru.korniltsev.telegram.chat.R;
import ru.korniltsev.telegram.core.views.AvatarView;
import rx.functions.Action1;

import java.util.ArrayList;
import java.util.List;

public class BotCommandsAdapter extends RecyclerView.Adapter<BotCommandsAdapter.VH> {
    final List<Record> commands = new ArrayList<>();
    private final LayoutInflater wFactory;
    @Nullable List<Record> filtered ;
    final Context ctx;
    final Action1<Record> clcikListener;
    public BotCommandsAdapter(List<Record> i, Context ctx, Action1<Record> clcikListener) {
        this.ctx = ctx;
        this.clcikListener = clcikListener;
        wFactory = LayoutInflater.from(ctx);
        commands.addAll(i);
    }

    public int filter(String text) {
        List<Record> filteredResult = new ArrayList<>();
        if (!text.isEmpty()){
            for (Record command : commands) {
                final String realCommand = "/" + command.cmd.command;
                if (realCommand.startsWith(text)
                        && text.length() <= realCommand.length()) {
                    filteredResult.add(command);
                }
            }
        }

        if (filteredResult.isEmpty()) {
            filtered = null;
        } else {
            filtered = filteredResult;
        }
        notifyDataSetChanged();
        return filteredResult.size();
    }

    @Override
    public BotCommandsAdapter.VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH(wFactory.inflate(R.layout.bot_command_item, parent, false));
    }

    @Override
    public void onBindViewHolder(BotCommandsAdapter.VH holder, int position) {
        final Record botCommand = getList().get(position);
        holder.cmd.setText("/" + botCommand.cmd.command);
        holder.description.setText(botCommand.cmd.description);
        holder.ava.loadAvatarFor(botCommand.user);

    }

    @Override
    public int getItemCount() {
        return getList().size();
    }

    private List<Record> getList() {
        if (filtered == null) {
            return commands;
        };
        return filtered;
    }

    class VH extends RecyclerView.ViewHolder{
        final AvatarView ava;
        final TextView cmd;
        final TextView description;

        public VH(View itemView) {
            super(itemView);
            ava = (AvatarView) itemView.findViewById(R.id.avatar);
            cmd = (TextView) itemView.findViewById(R.id.command);
            description = (TextView) itemView.findViewById(R.id.description);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Record record = getList().get(getAdapterPosition());
                    clcikListener.call(record);
                }
            });
        }
    }

    public static class Record {
        public final TdApi.User user;
        public final TdApi.BotCommand cmd;

        public Record(TdApi.User user, TdApi.BotCommand cmd) {
            this.user = user;
            this.cmd = cmd;
        }
    }
}
