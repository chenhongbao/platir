package io.platir.core.internal;

import io.platir.service.Contract;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Chen Hongbao
 */
class CheckReturn {

    private int code;
    private String message;
    private Set<Contract> contracts = new HashSet<>();

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Set<Contract> getContracts() {
        return contracts;
    }

    public void setContracts(Set<Contract> contracts) {
        this.contracts = contracts;
    }
}
