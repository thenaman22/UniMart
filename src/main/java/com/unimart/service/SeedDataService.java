package com.unimart.service;

import com.unimart.domain.Community;
import com.unimart.domain.CommunityDomain;
import com.unimart.domain.Membership;
import com.unimart.domain.MembershipRole;
import com.unimart.domain.MembershipStatus;
import com.unimart.domain.UserAccount;
import com.unimart.repository.CommunityDomainRepository;
import com.unimart.repository.CommunityRepository;
import com.unimart.repository.MembershipRepository;
import com.unimart.repository.UserAccountRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class SeedDataService {

    private final CommunityRepository communityRepository;
    private final CommunityDomainRepository communityDomainRepository;
    private final UserAccountRepository userAccountRepository;
    private final MembershipRepository membershipRepository;

    public SeedDataService(
        CommunityRepository communityRepository,
        CommunityDomainRepository communityDomainRepository,
        UserAccountRepository userAccountRepository,
        MembershipRepository membershipRepository
    ) {
        this.communityRepository = communityRepository;
        this.communityDomainRepository = communityDomainRepository;
        this.userAccountRepository = userAccountRepository;
        this.membershipRepository = membershipRepository;
    }

    @PostConstruct
    void seed() {
        if (communityRepository.count() > 0) {
            return;
        }

        Community community = new Community();
        community.setSlug("campus-market");
        community.setName("Campus Market");
        community.setDescription("Private marketplace for students and staff.");
        communityRepository.save(community);

        CommunityDomain domain = new CommunityDomain();
        domain.setCommunity(community);
        domain.setEmailDomain("school.edu");
        communityDomainRepository.save(domain);

        UserAccount admin = new UserAccount();
        admin.setDisplayName("Campus Admin");
        admin.setEmail("admin@school.edu");
        admin.setEmailVerified(true);
        userAccountRepository.save(admin);

        Membership membership = new Membership();
        membership.setUser(admin);
        membership.setCommunity(community);
        membership.setRole(MembershipRole.ADMIN);
        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);
    }
}
