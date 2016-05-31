package altitech.trackspace;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Created by Altitech_xyz on 19.04.16.
 */
public class MyDialogFragment extends DialogFragment implements View.OnClickListener{

    SeekBar seekBar;
    Button save;
    Button cancel;
    TextView seekBarValue;
    int currentValue;
    int result = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.settings_dialog, container, false);

        getDialog().setTitle(R.string.action_settings);
        setCancelable(false);

        seekBar = (SeekBar) v.findViewById(R.id.seekBarUpdateRate);
        seekBarValue = (TextView) v.findViewById(R.id.textUpdateRateOutput);
        save = (Button) v.findViewById(R.id.save_action);
        cancel = (Button) v.findViewById(R.id.cancel_action);

        currentValue = Main.updateInterval/1000;

        seekBarValue.setText(currentValue+"s");
        seekBar.setProgress(currentValue-5);

        save.setOnClickListener(this);
        cancel.setOnClickListener(this);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress+=5;

                seekBarValue.setText((progress)+"s");
                currentValue = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        return v;
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.save_action:

                Main.updateInterval = (currentValue)*1000;
                Log.i("onClick", "Intervall is "+(currentValue)+"s");
                synchronized (Main.thread) {
                    Main.thread.notify();
                }
                dismiss();
                break;
            case R.id.cancel_action:
                dismiss();
                break;
        }
    }
}
