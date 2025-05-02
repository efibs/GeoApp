namespace GeoAppAPI.Models;

public static class Permissions
{
    public const string PermissionClaimType = nameof(Permissions);
    
    public const string All = "perm:" + nameof(All);
    public const string Register = "perm:" + nameof(Register);
    public const string ReadData = "perm:" + nameof(ReadData);
    public const string WriteData = "perm:" + nameof(WriteData);
    public const string GenerateToken = "perm:" + nameof(GenerateToken);
    
    public static readonly string[] AvailablePermissions = [All, Register, ReadData, WriteData, GenerateToken];
}