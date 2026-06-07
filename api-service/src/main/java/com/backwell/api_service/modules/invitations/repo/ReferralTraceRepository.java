package com.backwell.api_service.modules.invitations.repo;

import com.backwell.api_service.modules.invitations.entity.InvitationTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReferralTraceRepository  extends JpaRepository<InvitationTrace, UUID> {

}
