package com.pvp.travelmatch.entity;
import jakarta.persistence.*; import lombok.*; import java.time.LocalDateTime;
@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(name="profile_views",uniqueConstraints=@UniqueConstraint(name="uk_profile_view_pair",columnNames={"viewer_id","viewed_user_id"}),indexes=@Index(name="idx_profile_viewed",columnList="viewed_user_id,viewed_at"))
public class ProfileView { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="viewer_id",nullable=false) private User viewer; @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="viewed_user_id",nullable=false) private User viewedUser; @Column(nullable=false) private LocalDateTime viewedAt; }
