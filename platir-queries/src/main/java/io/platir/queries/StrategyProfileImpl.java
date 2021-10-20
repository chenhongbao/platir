package io.platir.queries;

import io.platir.service.StrategyProfile;
import java.net.URI;
import java.util.Arrays;

/**
 *
 * @author Chen Hongbao
 */
class StrategyProfileImpl implements StrategyProfile {

    private String strategyProfileId;
    private String strategyId;
    private String userId;
    private String password;
    private String state;
    private String createDate;
    private String removeDate;
    private String[] instrumentIds;
    private String[] args;
    private URI uri;

    @Override
    public String getStrategyProfileId() {
        return strategyProfileId;
    }

    @Override
    public void setStrategyProfileId(String strategyProfileId) {
        this.strategyProfileId = strategyProfileId;
    }

    @Override
    public String getStrategyId() {
        return strategyId;
    }

    @Override
    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getState() {
        return state;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public void setState(String state) {
        this.state = state;
    }

    @Override
    public String getCreateDate() {
        return createDate;
    }

    @Override
    public void setCreateDate(String createDate) {
        this.createDate = createDate;
    }

    @Override
    public String getRemoveDate() {
        return removeDate;
    }

    @Override
    public void setRemoveDate(String removeDate) {
        this.removeDate = removeDate;
    }

    @Override
    public String[] getInstrumentIds() {
        return instrumentIds;
    }

    @Override
    public void setInstrumentIds(String[] instrumentIds) {
        if (instrumentIds != null && instrumentIds.length > 0) {
            this.instrumentIds = Arrays.copyOf(instrumentIds, instrumentIds.length);
        }
    }

    @Override
    public String[] getArgs() {
        return args;
    }

    @Override
    public void setArgs(String[] args) {
        if (args != null && args.length > 0) {
            this.args = Arrays.copyOf(args, args.length);
        }
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public void setUri(URI uri) {
        this.uri = uri;
    }

}
