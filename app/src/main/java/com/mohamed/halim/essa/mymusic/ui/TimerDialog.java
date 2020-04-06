package com.mohamed.halim.essa.mymusic.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.mohamed.halim.essa.mymusic.R;

public class TimerDialog extends AppCompatDialogFragment {
    private static final String TAG = "TimerDialog";
    private RadioGroup radioGroup;
    private EditText customTime;
    private int time;
    private TimerDialogListener timerDialogListener;

    public TimerDialog(TimerDialogListener timerDialogListener) {
        this.timerDialogListener = timerDialogListener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        View view = getActivity().getLayoutInflater().inflate(R.layout.timer_dialog, null);
        radioGroup = view.findViewById(R.id.timer_selection_group);
        customTime = view.findViewById(R.id.timer_custom_tv);
        dialogBuilder.setView(view)
                .setTitle(R.string.timer_dialog_title)
                .setNegativeButton(R.string.timer_dialog_neg, (dialog, which) -> dialog.dismiss())
                .setPositiveButton(getString(R.string.timer_dialog_pos), (dialog, which) -> {
                    int id = radioGroup.getCheckedRadioButtonId();
                    switch (id) {
                        case R.id.timer_15_min:
                            time = 15;
                            break;
                        case R.id.timer_30_min:
                            time = 30;
                            break;
                        case R.id.timer_1_hour:
                            time = 60;
                            break;
                        case R.id.timer_custom:

                            try {
                                Log.e(TAG, "onCreateDialog" + customTime.getText().toString());
                                time = Integer.valueOf(customTime.getText().toString());
                            } catch (NumberFormatException nfe) {
                                dialog.dismiss();
                                return;
                            }
                            break;
                        default:
                            dialog.dismiss();
                            return;
                    }
                    Log.e(TAG, "onCreateDialog" + customTime.getText().toString());
                    Log.e(TAG, "onCreateDialog: " + time);
                    timerDialogListener.onSetTime(time);
                });
        return dialogBuilder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

    }

    public interface TimerDialogListener {
        void onSetTime(int time);
    }
}
