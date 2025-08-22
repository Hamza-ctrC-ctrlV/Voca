package com.ensapfe.voca.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ensapfe.voca.R;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_AI = 2;

    private List<ChatFragment.ChatMessage> messageList;

    public ChatAdapter(List<ChatFragment.ChatMessage> messageList) {
        this.messageList = messageList;
        setHasStableIds(false);
    }

    @Override
    public int getItemViewType(int position) {
        if (position < 0 || position >= messageList.size()) {
            return VIEW_TYPE_AI;
        }
        ChatFragment.ChatMessage message = messageList.get(position);
        return message.isUser() ? VIEW_TYPE_USER : VIEW_TYPE_AI;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;

        try {
            if (viewType == VIEW_TYPE_USER) {
                view = inflater.inflate(R.layout.item_chat_user, parent, false);
            } else {
                view = inflater.inflate(R.layout.item_chat_ai, parent, false);
            }
        } catch (Exception e) {
            view = inflater.inflate(R.layout.item_chat_ai, parent, false);
        }

        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        if (position < 0 || position >= messageList.size()) {
            return;
        }

        ChatFragment.ChatMessage message = messageList.get(position);
        if (message != null && holder.messageTextView != null) {
            holder.messageTextView.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.message_text);
        }
    }
}