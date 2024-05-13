// ILinkPOSService.aidl
package th.co.bkkps.linkpos;

// Declare any non-default types here with import statements

interface ILinkPOSService {
    void call(in String jsonMessage);
    boolean checkOnProcessing();
    boolean checkOnHomeScreen();
}
