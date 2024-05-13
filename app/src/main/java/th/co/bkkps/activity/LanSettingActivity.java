package th.co.bkkps.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pax.dal.IDalCommManager;
import com.pax.dal.entity.LanParam;
import com.pax.edc.R;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.utils.Utils;

public class LanSettingActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    LanParam lanParam;
    EditText et_Ip;
    EditText et_Subnet;
    EditText et_Gateway;
    EditText et_Dns1;
    EditText et_Dns2;

    Switch dhcpSwitch;
    Button btnSet;
    Button btnCancel;

    IDalCommManager commManager;

    String defaultDns1 = "8.8.8.8";
    String defaultDns2 = "8.8.4.4";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_lan);
        commManager = FinancialApplication.getDal().getCommManager();
        lanParam = commManager.getLanConfig();
        initView();
    }

    private void initView() {
        et_Ip = findViewById(R.id.editTextTextIp);
        et_Subnet = findViewById(R.id.editTextTextSubnetMask);
        et_Gateway = findViewById(R.id.editTextTextGateway);
        et_Dns1 = findViewById(R.id.editTextTextDNS1);
        et_Dns2 = findViewById(R.id.editTextTextDNS2);
        dhcpSwitch = findViewById(R.id.switch1);
        btnSet = findViewById(R.id.button);
        btnCancel = findViewById(R.id.button2);
        btnSet.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        et_Ip.setText(lanParam.getLocalIp());
        et_Subnet.setText(lanParam.getSubnetMask());
        et_Gateway.setText(lanParam.getGateway());
        et_Dns1.setText(lanParam.getDns1());
        et_Dns2.setText(lanParam.getDns2());
        dhcpSwitch.setChecked(lanParam.isDhcp());
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.button:
                lanParam.setLocalIp(et_Ip.getText().toString());
                lanParam.setSubnetMask(et_Subnet.getText().toString());
                lanParam.setGateway(et_Gateway.getText().toString());
                String dns1 = et_Dns1.getText().toString().isEmpty()? defaultDns1 : et_Dns1.getText().toString();
                lanParam.setDns1(dns1);

                String dns2 = et_Dns2.getText().toString().isEmpty()? defaultDns2 : et_Dns2.getText().toString();
                lanParam.setDns2(dns2);

                lanParam.setDhcp(dhcpSwitch.isChecked());
                Utils.updateAndSaveLanParam(lanParam);


                finish();
                break;
            case R.id.button2:
                finish();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
