package ch.ethz.twimight.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import ch.ethz.twimight.R;

import static ch.ethz.twimight.activities.HomeScreenActivity.PHONE_NO;

public class TipsActivity extends Activity  {
	EditText phone;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tips);
		phone = (EditText)findViewById(R.id.phone_no);
		Button save = (Button)findViewById(R.id.buttonSkip);
		save.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final String phoneTextVal = phone.getText().toString();
				Log.v("pn",phoneTextVal);
				if(phoneTextVal.length() == 10 && phoneTextVal.matches("^[789]\\d{9}$")) {
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TipsActivity.this);
					SharedPreferences.Editor prefEditor = prefs.edit();
					prefEditor.putString(PHONE_NO, phoneTextVal);
					prefEditor.commit();

					advanceToLogin(v);
				}
				else {
					phone.setError("Enter Valid Number");
				}
			}
		});
	}
	
	/**
	 * Starts the login activity. This method is called from the agree button's onClick
	 * listener set in the layout.
	 * 
	 * @param unused obligatory View argument for onClick callback methods
	 */
	public void advanceToLogin(View unused) {
		Intent intent = new Intent(this, LoginActivity.class);
		startActivity(intent);
		finish();
	}
}
