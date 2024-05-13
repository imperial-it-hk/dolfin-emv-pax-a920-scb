package th.co.bkkps.scbapi.trans.action.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pax.abl.core.AAction;
import com.pax.abl.core.ActionResult;
import com.pax.edc.opensdk.TransResult;
import com.pax.pay.app.FinancialApplication;
import com.pax.pay.trans.TransContext;
import com.pax.settings.SysParam;

import java.io.File;

import th.co.bkkps.bpsapi.LoadTleMsg;
import th.co.bkkps.bpsapi.TransAPIFactory;
import th.co.bkkps.scbapi.ScbIppService;

public class ScbIppLoadTle extends AppCompatActivity  {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!ScbIppService.isSCBInstalled(this)) {
            finish(new ActionResult(TransResult.ERR_SCB_CONNECTION, null));
            return;
        }

        String jsonTe = null;

//        StringBuilder sb = new StringBuilder();
//        try {
//            InputStream is = getAssets().open("teid.json");
//            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8 ));
//
//            while ((jsonTe = br.readLine()) != null) {
//                sb.append(jsonTe);
//            }
//            jsonTe = sb.toString();
//            br.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        String tleFilePath = FinancialApplication.getSysParam().get(SysParam.StringParam.TLE_PARAMETER_FILE_PATH);
        File f = new File(tleFilePath);
        if (f.exists() && !f.isDirectory()) {

            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode jsonNode = mapper.readTree(f);
                jsonTe = jsonNode.toString();
            } catch (Exception e) {
                e.printStackTrace();
                finish();
                return;
            }
        }

        LoadTleMsg.Request loadTleRequest = new LoadTleMsg.Request();
        loadTleRequest.setJsonTe(jsonTe);
        TransAPIFactory.createTransAPI().startTrans(this, loadTleRequest);
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        LoadTleMsg.Response loadTleRes = (LoadTleMsg.Response) TransAPIFactory.createTransAPI().onResult(requestCode, resultCode, data);

        finish(new ActionResult(0, null));
    }

    public void finish(ActionResult result) {
        AAction action = TransContext.getInstance().getCurrentAction();
        if (action != null) {
            if (action.isFinished())
                return;
            action.setFinished(true);
            action.setResult(result);
        }

        finish();
    }
}
