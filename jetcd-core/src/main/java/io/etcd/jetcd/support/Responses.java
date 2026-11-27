/*
 * Copyright 2016-2021 The jetcd authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.etcd.jetcd.support;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.api.DeleteRangeResponse;
import io.etcd.jetcd.api.RangeResponse;
import io.etcd.jetcd.auth.AuthDisableResponse;
import io.etcd.jetcd.auth.AuthEnableResponse;
import io.etcd.jetcd.auth.AuthRoleAddResponse;
import io.etcd.jetcd.auth.AuthRoleDeleteResponse;
import io.etcd.jetcd.auth.AuthRoleGetResponse;
import io.etcd.jetcd.auth.AuthRoleGrantPermissionResponse;
import io.etcd.jetcd.auth.AuthRoleListResponse;
import io.etcd.jetcd.auth.AuthRoleRevokePermissionResponse;
import io.etcd.jetcd.auth.AuthUserAddResponse;
import io.etcd.jetcd.auth.AuthUserChangePasswordResponse;
import io.etcd.jetcd.auth.AuthUserDeleteResponse;
import io.etcd.jetcd.auth.AuthUserGetResponse;
import io.etcd.jetcd.auth.AuthUserGrantRoleResponse;
import io.etcd.jetcd.auth.AuthUserListResponse;
import io.etcd.jetcd.auth.AuthUserRevokeRoleResponse;
import io.etcd.jetcd.cluster.MemberAddResponse;
import io.etcd.jetcd.cluster.MemberListResponse;
import io.etcd.jetcd.cluster.MemberPromoteResponse;
import io.etcd.jetcd.cluster.MemberRemoveResponse;
import io.etcd.jetcd.cluster.MemberUpdateResponse;
import io.etcd.jetcd.election.CampaignResponse;
import io.etcd.jetcd.election.LeaderResponse;
import io.etcd.jetcd.election.ProclaimResponse;
import io.etcd.jetcd.election.ResignResponse;
import io.etcd.jetcd.kv.CompactResponse;
import io.etcd.jetcd.kv.DeleteResponse;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.kv.TxnResponse;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseRevokeResponse;
import io.etcd.jetcd.lease.LeaseTimeToLiveResponse;
import io.etcd.jetcd.lock.LockResponse;
import io.etcd.jetcd.lock.UnlockResponse;
import io.etcd.jetcd.maintenance.AlarmResponse;
import io.etcd.jetcd.maintenance.DefragmentResponse;
import io.etcd.jetcd.maintenance.HashKVResponse;
import io.etcd.jetcd.maintenance.HashResponse;
import io.etcd.jetcd.maintenance.MoveLeaderResponse;
import io.etcd.jetcd.maintenance.SnapshotResponse;
import io.etcd.jetcd.maintenance.StatusResponse;
import io.etcd.jetcd.watch.WatchResponse;

/**
 * Factory for creating Response objects.
 * Provides static factory methods for direct response creation and a namespaced factory for method references.
 */
public final class Responses {

    private Responses() {
    }

    // ============================================
    // Static factory methods (with namespace)
    // ============================================

    /**
     * Creates a PutResponse with namespace.
     *
     * @param  response  the gRPC put response
     * @param  namespace the namespace
     * @return           the PutResponse
     */
    public static PutResponse newPutResponse(io.etcd.jetcd.api.PutResponse response, ByteSequence namespace) {
        return new PutResponse(response, namespace);
    }

    /**
     * Creates a GetResponse with namespace.
     *
     * @param  response  the gRPC range response
     * @param  namespace the namespace
     * @return           the GetResponse
     */
    public static GetResponse newGetResponse(RangeResponse response, ByteSequence namespace) {
        return new GetResponse(response, namespace);
    }

    /**
     * Creates a DeleteResponse with namespace.
     *
     * @param  response  the gRPC delete range response
     * @param  namespace the namespace
     * @return           the DeleteResponse
     */
    public static DeleteResponse newDeleteResponse(DeleteRangeResponse response, ByteSequence namespace) {
        return new DeleteResponse(response, namespace);
    }

    /**
     * Creates a TxnResponse with namespace.
     *
     * @param  response  the gRPC transaction response
     * @param  namespace the namespace
     * @return           the TxnResponse
     */
    public static TxnResponse newTxnResponse(io.etcd.jetcd.api.TxnResponse response, ByteSequence namespace) {
        return new TxnResponse(response, namespace);
    }

    /**
     * Creates a WatchResponse with namespace.
     *
     * @param  response  the gRPC watch response
     * @param  namespace the namespace
     * @return           the WatchResponse
     */
    public static WatchResponse newWatchResponse(io.etcd.jetcd.api.WatchResponse response, ByteSequence namespace) {
        return new WatchResponse(response, namespace);
    }

    /**
     * Creates a LeaderResponse with namespace.
     *
     * @param  response  the gRPC leader response
     * @param  namespace the namespace
     * @return           the LeaderResponse
     */
    public static LeaderResponse newLeaderResponse(io.etcd.jetcd.api.LeaderResponse response, ByteSequence namespace) {
        return new LeaderResponse(response, namespace);
    }

    /**
     * Creates a LockResponse with namespace.
     *
     * @param  response  the gRPC lock response
     * @param  namespace the namespace
     * @return           the LockResponse
     */
    public static LockResponse newLockResponse(io.etcd.jetcd.api.lock.LockResponse response, ByteSequence namespace) {
        return new LockResponse(response, namespace);
    }

    // ============================================
    // Static factory methods (without namespace)
    // ============================================

    /**
     * Creates a CompactResponse.
     *
     * @param  response the gRPC compaction response
     * @return          the CompactResponse
     */
    public static CompactResponse newCompactResponse(io.etcd.jetcd.api.CompactionResponse response) {
        return new CompactResponse(response);
    }

    /**
     * Creates a LeaseGrantResponse.
     *
     * @param  response the gRPC lease grant response
     * @return          the LeaseGrantResponse
     */
    public static LeaseGrantResponse newLeaseGrantResponse(io.etcd.jetcd.api.LeaseGrantResponse response) {
        return new LeaseGrantResponse(response);
    }

    /**
     * Creates a LeaseRevokeResponse.
     *
     * @param  response the gRPC lease revoke response
     * @return          the LeaseRevokeResponse
     */
    public static LeaseRevokeResponse newLeaseRevokeResponse(io.etcd.jetcd.api.LeaseRevokeResponse response) {
        return new LeaseRevokeResponse(response);
    }

    /**
     * Creates a LeaseTimeToLiveResponse.
     *
     * @param  response the gRPC lease time to live response
     * @return          the LeaseTimeToLiveResponse
     */
    public static LeaseTimeToLiveResponse newLeaseTimeToLiveResponse(
        io.etcd.jetcd.api.LeaseTimeToLiveResponse response) {
        return new LeaseTimeToLiveResponse(response);
    }

    /**
     * Creates a CampaignResponse.
     *
     * @param  response the gRPC campaign response
     * @return          the CampaignResponse
     */
    public static CampaignResponse newCampaignResponse(io.etcd.jetcd.api.CampaignResponse response) {
        return new CampaignResponse(response);
    }

    /**
     * Creates a ProclaimResponse.
     *
     * @param  response the gRPC proclaim response
     * @return          the ProclaimResponse
     */
    public static ProclaimResponse newProclaimResponse(io.etcd.jetcd.api.ProclaimResponse response) {
        return new ProclaimResponse(response);
    }

    /**
     * Creates a ResignResponse.
     *
     * @param  response the gRPC resign response
     * @return          the ResignResponse
     */
    public static ResignResponse newResignResponse(io.etcd.jetcd.api.ResignResponse response) {
        return new ResignResponse(response);
    }

    /**
     * Creates an UnlockResponse.
     *
     * @param  response the gRPC unlock response
     * @return          the UnlockResponse
     */
    public static UnlockResponse newUnlockResponse(io.etcd.jetcd.api.lock.UnlockResponse response) {
        return new UnlockResponse(response);
    }

    /**
     * Creates an AlarmResponse.
     *
     * @param  response the gRPC alarm response
     * @return          the AlarmResponse
     */
    public static AlarmResponse newAlarmResponse(io.etcd.jetcd.api.AlarmResponse response) {
        return new AlarmResponse(response);
    }

    /**
     * Creates a DefragmentResponse.
     *
     * @param  response the gRPC defragment response
     * @return          the DefragmentResponse
     */
    public static DefragmentResponse newDefragmentResponse(io.etcd.jetcd.api.DefragmentResponse response) {
        return new DefragmentResponse(response);
    }

    /**
     * Creates a HashResponse.
     *
     * @param  response the gRPC hash response
     * @return          the HashResponse
     */
    public static HashResponse newHashResponse(io.etcd.jetcd.api.HashResponse response) {
        return new HashResponse(response);
    }

    /**
     * Creates a HashKVResponse.
     *
     * @param  response the gRPC hash KV response
     * @return          the HashKVResponse
     */
    public static HashKVResponse newHashKVResponse(io.etcd.jetcd.api.HashKVResponse response) {
        return new HashKVResponse(response);
    }

    /**
     * Creates a MoveLeaderResponse.
     *
     * @param  response the gRPC move leader response
     * @return          the MoveLeaderResponse
     */
    public static MoveLeaderResponse newMoveLeaderResponse(io.etcd.jetcd.api.MoveLeaderResponse response) {
        return new MoveLeaderResponse(response);
    }

    /**
     * Creates a SnapshotResponse.
     *
     * @param  response the gRPC snapshot response
     * @return          the SnapshotResponse
     */
    public static SnapshotResponse newSnapshotResponse(io.etcd.jetcd.api.SnapshotResponse response) {
        return new SnapshotResponse(response);
    }

    /**
     * Creates a StatusResponse.
     *
     * @param  response the gRPC status response
     * @return          the StatusResponse
     */
    public static StatusResponse newStatusResponse(io.etcd.jetcd.api.StatusResponse response) {
        return new StatusResponse(response);
    }

    /**
     * Creates a MemberAddResponse.
     *
     * @param  response the gRPC member add response
     * @return          the MemberAddResponse
     */
    public static MemberAddResponse newMemberAddResponse(io.etcd.jetcd.api.MemberAddResponse response) {
        return new MemberAddResponse(response);
    }

    /**
     * Creates a MemberListResponse.
     *
     * @param  response the gRPC member list response
     * @return          the MemberListResponse
     */
    public static MemberListResponse newMemberListResponse(io.etcd.jetcd.api.MemberListResponse response) {
        return new MemberListResponse(response);
    }

    /**
     * Creates a MemberPromoteResponse.
     *
     * @param  response the gRPC member promote response
     * @return          the MemberPromoteResponse
     */
    public static MemberPromoteResponse newMemberPromoteResponse(io.etcd.jetcd.api.MemberPromoteResponse response) {
        return new MemberPromoteResponse(response);
    }

    /**
     * Creates a MemberRemoveResponse.
     *
     * @param  response the gRPC member remove response
     * @return          the MemberRemoveResponse
     */
    public static MemberRemoveResponse newMemberRemoveResponse(io.etcd.jetcd.api.MemberRemoveResponse response) {
        return new MemberRemoveResponse(response);
    }

    /**
     * Creates a MemberUpdateResponse.
     *
     * @param  response the gRPC member update response
     * @return          the MemberUpdateResponse
     */
    public static MemberUpdateResponse newMemberUpdateResponse(io.etcd.jetcd.api.MemberUpdateResponse response) {
        return new MemberUpdateResponse(response);
    }

    /**
     * Creates an AuthDisableResponse.
     *
     * @param  response the gRPC auth disable response
     * @return          the AuthDisableResponse
     */
    public static AuthDisableResponse newAuthDisableResponse(io.etcd.jetcd.api.AuthDisableResponse response) {
        return new AuthDisableResponse(response);
    }

    /**
     * Creates an AuthEnableResponse.
     *
     * @param  response the gRPC auth enable response
     * @return          the AuthEnableResponse
     */
    public static AuthEnableResponse newAuthEnableResponse(io.etcd.jetcd.api.AuthEnableResponse response) {
        return new AuthEnableResponse(response);
    }

    /**
     * Creates an AuthRoleAddResponse.
     *
     * @param  response the gRPC auth role add response
     * @return          the AuthRoleAddResponse
     */
    public static AuthRoleAddResponse newAuthRoleAddResponse(io.etcd.jetcd.api.AuthRoleAddResponse response) {
        return new AuthRoleAddResponse(response);
    }

    /**
     * Creates an AuthRoleDeleteResponse.
     *
     * @param  response the gRPC auth role delete response
     * @return          the AuthRoleDeleteResponse
     */
    public static AuthRoleDeleteResponse newAuthRoleDeleteResponse(io.etcd.jetcd.api.AuthRoleDeleteResponse response) {
        return new AuthRoleDeleteResponse(response);
    }

    /**
     * Creates an AuthRoleGetResponse.
     *
     * @param  response the gRPC auth role get response
     * @return          the AuthRoleGetResponse
     */
    public static AuthRoleGetResponse newAuthRoleGetResponse(io.etcd.jetcd.api.AuthRoleGetResponse response) {
        return new AuthRoleGetResponse(response);
    }

    /**
     * Creates an AuthRoleGrantPermissionResponse.
     *
     * @param  response the gRPC auth role grant permission response
     * @return          the AuthRoleGrantPermissionResponse
     */
    public static AuthRoleGrantPermissionResponse newAuthRoleGrantPermissionResponse(
        io.etcd.jetcd.api.AuthRoleGrantPermissionResponse response) {
        return new AuthRoleGrantPermissionResponse(response);
    }

    /**
     * Creates an AuthRoleListResponse.
     *
     * @param  response the gRPC auth role list response
     * @return          the AuthRoleListResponse
     */
    public static AuthRoleListResponse newAuthRoleListResponse(io.etcd.jetcd.api.AuthRoleListResponse response) {
        return new AuthRoleListResponse(response);
    }

    /**
     * Creates an AuthRoleRevokePermissionResponse.
     *
     * @param  response the gRPC auth role revoke permission response
     * @return          the AuthRoleRevokePermissionResponse
     */
    public static AuthRoleRevokePermissionResponse newAuthRoleRevokePermissionResponse(
        io.etcd.jetcd.api.AuthRoleRevokePermissionResponse response) {
        return new AuthRoleRevokePermissionResponse(response);
    }

    /**
     * Creates an AuthUserAddResponse.
     *
     * @param  response the gRPC auth user add response
     * @return          the AuthUserAddResponse
     */
    public static AuthUserAddResponse newAuthUserAddResponse(io.etcd.jetcd.api.AuthUserAddResponse response) {
        return new AuthUserAddResponse(response);
    }

    /**
     * Creates an AuthUserChangePasswordResponse.
     *
     * @param  response the gRPC auth user change password response
     * @return          the AuthUserChangePasswordResponse
     */
    public static AuthUserChangePasswordResponse newAuthUserChangePasswordResponse(
        io.etcd.jetcd.api.AuthUserChangePasswordResponse response) {
        return new AuthUserChangePasswordResponse(response);
    }

    /**
     * Creates an AuthUserDeleteResponse.
     *
     * @param  response the gRPC auth user delete response
     * @return          the AuthUserDeleteResponse
     */
    public static AuthUserDeleteResponse newAuthUserDeleteResponse(io.etcd.jetcd.api.AuthUserDeleteResponse response) {
        return new AuthUserDeleteResponse(response);
    }

    /**
     * Creates an AuthUserGetResponse.
     *
     * @param  response the gRPC auth user get response
     * @return          the AuthUserGetResponse
     */
    public static AuthUserGetResponse newAuthUserGetResponse(io.etcd.jetcd.api.AuthUserGetResponse response) {
        return new AuthUserGetResponse(response);
    }

    /**
     * Creates an AuthUserGrantRoleResponse.
     *
     * @param  response the gRPC auth user grant role response
     * @return          the AuthUserGrantRoleResponse
     */
    public static AuthUserGrantRoleResponse newAuthUserGrantRoleResponse(
        io.etcd.jetcd.api.AuthUserGrantRoleResponse response) {
        return new AuthUserGrantRoleResponse(response);
    }

    /**
     * Creates an AuthUserListResponse.
     *
     * @param  response the gRPC auth user list response
     * @return          the AuthUserListResponse
     */
    public static AuthUserListResponse newAuthUserListResponse(io.etcd.jetcd.api.AuthUserListResponse response) {
        return new AuthUserListResponse(response);
    }

    /**
     * Creates an AuthUserRevokeRoleResponse.
     *
     * @param  response the gRPC auth user revoke role response
     * @return          the AuthUserRevokeRoleResponse
     */
    public static AuthUserRevokeRoleResponse newAuthUserRevokeRoleResponse(
        io.etcd.jetcd.api.AuthUserRevokeRoleResponse response) {
        return new AuthUserRevokeRoleResponse(response);
    }

    // ============================================
    // Namespaced factory for method references
    // ============================================

    /**
     * Returns a namespaced factory for creating namespace-aware responses.
     * Use this to cache the factory in client code for cleaner method references.
     *
     * @param  namespace the namespace for response construction
     * @return           a namespaced factory instance
     */
    public static Namespaced namespaced(ByteSequence namespace) {
        return new Namespaced(namespace);
    }

    /**
     * Factory for creating namespace-aware Response objects.
     * Designed to be cached in client implementations for method reference usage.
     *
     * @param namespace the namespace to use for responses
     */
    public record Namespaced(ByteSequence namespace) {

        /**
         * Creates PutResponse with namespace.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public PutResponse newPutResponse(io.etcd.jetcd.api.PutResponse response) {
            return Responses.newPutResponse(response, namespace);
        }

        /**
         * Creates GetResponse with namespace.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public GetResponse newGetResponse(RangeResponse response) {
            return Responses.newGetResponse(response, namespace);
        }

        /**
         * Creates DeleteResponse with namespace.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public DeleteResponse newDeleteResponse(DeleteRangeResponse response) {
            return Responses.newDeleteResponse(response, namespace);
        }

        /**
         * Creates TxnResponse with namespace.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public TxnResponse newTxnResponse(io.etcd.jetcd.api.TxnResponse response) {
            return Responses.newTxnResponse(response, namespace);
        }

        /**
         * Creates WatchResponse with namespace.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public WatchResponse newWatchResponse(io.etcd.jetcd.api.WatchResponse response) {
            return Responses.newWatchResponse(response, namespace);
        }

        /**
         * Creates LeaderResponse with namespace.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public LeaderResponse newLeaderResponse(io.etcd.jetcd.api.LeaderResponse response) {
            return Responses.newLeaderResponse(response, namespace);
        }

        /**
         * Creates LockResponse with namespace.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public LockResponse newLockResponse(io.etcd.jetcd.api.lock.LockResponse response) {
            return Responses.newLockResponse(response, namespace);
        }

        /**
         * Creates CompactResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public CompactResponse newCompactResponse(io.etcd.jetcd.api.CompactionResponse response) {
            return Responses.newCompactResponse(response);
        }

        /**
         * Creates LeaseGrantResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public LeaseGrantResponse newLeaseGrantResponse(io.etcd.jetcd.api.LeaseGrantResponse response) {
            return Responses.newLeaseGrantResponse(response);
        }

        /**
         * Creates LeaseRevokeResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public LeaseRevokeResponse newLeaseRevokeResponse(io.etcd.jetcd.api.LeaseRevokeResponse response) {
            return Responses.newLeaseRevokeResponse(response);
        }

        /**
         * Creates LeaseTimeToLiveResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public LeaseTimeToLiveResponse newLeaseTimeToLiveResponse(io.etcd.jetcd.api.LeaseTimeToLiveResponse response) {
            return Responses.newLeaseTimeToLiveResponse(response);
        }

        /**
         * Creates CampaignResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public CampaignResponse newCampaignResponse(io.etcd.jetcd.api.CampaignResponse response) {
            return Responses.newCampaignResponse(response);
        }

        /**
         * Creates ProclaimResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public ProclaimResponse newProclaimResponse(io.etcd.jetcd.api.ProclaimResponse response) {
            return Responses.newProclaimResponse(response);
        }

        /**
         * Creates ResignResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public ResignResponse newResignResponse(io.etcd.jetcd.api.ResignResponse response) {
            return Responses.newResignResponse(response);
        }

        /**
         * Creates UnlockResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public UnlockResponse newUnlockResponse(io.etcd.jetcd.api.lock.UnlockResponse response) {
            return Responses.newUnlockResponse(response);
        }

        /**
         * Creates AlarmResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AlarmResponse newAlarmResponse(io.etcd.jetcd.api.AlarmResponse response) {
            return Responses.newAlarmResponse(response);
        }

        /**
         * Creates DefragmentResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public DefragmentResponse newDefragmentResponse(io.etcd.jetcd.api.DefragmentResponse response) {
            return Responses.newDefragmentResponse(response);
        }

        /**
         * Creates HashResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public HashResponse newHashResponse(io.etcd.jetcd.api.HashResponse response) {
            return Responses.newHashResponse(response);
        }

        /**
         * Creates HashKVResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public HashKVResponse newHashKVResponse(io.etcd.jetcd.api.HashKVResponse response) {
            return Responses.newHashKVResponse(response);
        }

        /**
         * Creates MoveLeaderResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public MoveLeaderResponse newMoveLeaderResponse(io.etcd.jetcd.api.MoveLeaderResponse response) {
            return Responses.newMoveLeaderResponse(response);
        }

        /**
         * Creates SnapshotResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public SnapshotResponse newSnapshotResponse(io.etcd.jetcd.api.SnapshotResponse response) {
            return Responses.newSnapshotResponse(response);
        }

        /**
         * Creates StatusResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public StatusResponse newStatusResponse(io.etcd.jetcd.api.StatusResponse response) {
            return Responses.newStatusResponse(response);
        }

        /**
         * Creates MemberAddResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public MemberAddResponse newMemberAddResponse(io.etcd.jetcd.api.MemberAddResponse response) {
            return Responses.newMemberAddResponse(response);
        }

        /**
         * Creates MemberListResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public MemberListResponse newMemberListResponse(io.etcd.jetcd.api.MemberListResponse response) {
            return Responses.newMemberListResponse(response);
        }

        /**
         * Creates MemberPromoteResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public MemberPromoteResponse newMemberPromoteResponse(io.etcd.jetcd.api.MemberPromoteResponse response) {
            return Responses.newMemberPromoteResponse(response);
        }

        /**
         * Creates MemberRemoveResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public MemberRemoveResponse newMemberRemoveResponse(io.etcd.jetcd.api.MemberRemoveResponse response) {
            return Responses.newMemberRemoveResponse(response);
        }

        /**
         * Creates MemberUpdateResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public MemberUpdateResponse newMemberUpdateResponse(io.etcd.jetcd.api.MemberUpdateResponse response) {
            return Responses.newMemberUpdateResponse(response);
        }

        /**
         * Creates AuthDisableResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthDisableResponse newAuthDisableResponse(io.etcd.jetcd.api.AuthDisableResponse response) {
            return Responses.newAuthDisableResponse(response);
        }

        /**
         * Creates AuthEnableResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthEnableResponse newAuthEnableResponse(io.etcd.jetcd.api.AuthEnableResponse response) {
            return Responses.newAuthEnableResponse(response);
        }

        /**
         * Creates AuthRoleAddResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthRoleAddResponse newAuthRoleAddResponse(io.etcd.jetcd.api.AuthRoleAddResponse response) {
            return Responses.newAuthRoleAddResponse(response);
        }

        /**
         * Creates AuthRoleDeleteResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthRoleDeleteResponse newAuthRoleDeleteResponse(io.etcd.jetcd.api.AuthRoleDeleteResponse response) {
            return Responses.newAuthRoleDeleteResponse(response);
        }

        /**
         * Creates AuthRoleGetResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthRoleGetResponse newAuthRoleGetResponse(io.etcd.jetcd.api.AuthRoleGetResponse response) {
            return Responses.newAuthRoleGetResponse(response);
        }

        /**
         * Creates AuthRoleGrantPermissionResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthRoleGrantPermissionResponse newAuthRoleGrantPermissionResponse(
            io.etcd.jetcd.api.AuthRoleGrantPermissionResponse response) {
            return Responses.newAuthRoleGrantPermissionResponse(response);
        }

        /**
         * Creates AuthRoleListResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthRoleListResponse newAuthRoleListResponse(io.etcd.jetcd.api.AuthRoleListResponse response) {
            return Responses.newAuthRoleListResponse(response);
        }

        /**
         * Creates AuthRoleRevokePermissionResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthRoleRevokePermissionResponse newAuthRoleRevokePermissionResponse(
            io.etcd.jetcd.api.AuthRoleRevokePermissionResponse response) {
            return Responses.newAuthRoleRevokePermissionResponse(response);
        }

        /**
         * Creates AuthUserAddResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthUserAddResponse newAuthUserAddResponse(io.etcd.jetcd.api.AuthUserAddResponse response) {
            return Responses.newAuthUserAddResponse(response);
        }

        /**
         * Creates AuthUserChangePasswordResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthUserChangePasswordResponse newAuthUserChangePasswordResponse(
            io.etcd.jetcd.api.AuthUserChangePasswordResponse response) {
            return Responses.newAuthUserChangePasswordResponse(response);
        }

        /**
         * Creates AuthUserDeleteResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthUserDeleteResponse newAuthUserDeleteResponse(io.etcd.jetcd.api.AuthUserDeleteResponse response) {
            return Responses.newAuthUserDeleteResponse(response);
        }

        /**
         * Creates AuthUserGetResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthUserGetResponse newAuthUserGetResponse(io.etcd.jetcd.api.AuthUserGetResponse response) {
            return Responses.newAuthUserGetResponse(response);
        }

        /**
         * Creates AuthUserGrantRoleResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthUserGrantRoleResponse newAuthUserGrantRoleResponse(
            io.etcd.jetcd.api.AuthUserGrantRoleResponse response) {
            return Responses.newAuthUserGrantRoleResponse(response);
        }

        /**
         * Creates AuthUserListResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthUserListResponse newAuthUserListResponse(io.etcd.jetcd.api.AuthUserListResponse response) {
            return Responses.newAuthUserListResponse(response);
        }

        /**
         * Creates AuthUserRevokeRoleResponse.
         *
         * @param  response gRPC response
         * @return          response wrapper
         */
        public AuthUserRevokeRoleResponse newAuthUserRevokeRoleResponse(
            io.etcd.jetcd.api.AuthUserRevokeRoleResponse response) {
            return Responses.newAuthUserRevokeRoleResponse(response);
        }
    }
}
