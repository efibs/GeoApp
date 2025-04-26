using Microsoft.AspNetCore.Authorization;

namespace GeoAppAPI.Auth;

public class PermissionRequirement(string permission) : IAuthorizationRequirement
{
    public string Permission { get; } = permission;
}