package th.co.bkkps.linkposapi.util

import android.util.Log
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import th.co.bkkps.linkposapi.model.EDCLinkPOSParam
import th.co.bkkps.linkposapi.model.LinkPOSModel

object LinkPOSConvertMessage {
    const val TAG = "LinkPOSConvertMessage"

    fun modelToJson(model: LinkPOSModel): String? {
        try {
            return Json.encodeToString(model)
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
        return null
    }

    fun jsonToModel(jsonMsg: String): LinkPOSModel? {
        try {
            return Json.decodeFromString<LinkPOSModel>(jsonMsg)
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
        return null
    }

    fun paramToJson(param: EDCLinkPOSParam): String? {
        try {
            return Json.encodeToString(param)
        } catch (e: Exception) {
            Log.e(TAG, "", e)
        }
        return null
    }

//    fun jsonToArray(jsonMsg: String): List<RequestFields>? {
//        try {
//            return Json.decodeFromString<List<RequestFields>>(jsonMsg)
//        } catch (e: Exception) {
//            Log.e(TAG, "", e)
//        }
//        return null
//    }
}