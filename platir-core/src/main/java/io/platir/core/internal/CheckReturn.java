package io.platir.core.internal;

import io.platir.service.Contract;
import io.platir.service.Notice;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Chen Hongbao
 */
class CheckReturn {

    private Notice notice;
    private Set<Contract> contracts = new HashSet<>();

    public Notice getNotice() {
        return notice;
    }

    public void setNotice(Notice notice) {
        this.notice = notice;
    }

    public Set<Contract> getContracts() {
        return contracts;
    }

    public void setContracts(Set<Contract> contracts) {
        this.contracts = contracts;
    }

}
