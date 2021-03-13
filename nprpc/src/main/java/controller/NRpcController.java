package controller;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

public class NRpcController implements RpcController {
    private String errText;
    private boolean isFailed;

    @Override
    public void reset() {
        this.isFailed = false;
        this.errText = null;
    }

    @Override
    public boolean failed() {
        return this.isFailed;
    }

    @Override
    public String errorText() {
        return this.errText;
    }

    @Override
    public void startCancel() {

    }

    @Override
    public void setFailed(String reason) {
        this.isFailed = true;
        this.errText = reason;
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void notifyOnCancel(RpcCallback<Object> callback) {

    }
}
