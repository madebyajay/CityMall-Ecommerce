import { useEffect, useState } from "react";
import { useAuth } from "../../context/AuthContext";
import { userApi } from "../../services/userService";
import { extractErrorMessage } from "../../services/authService";
import Loader from "../../components/Loader/Loader";
import "./Profile.css";

const emptyAddress = {
  label: "HOME",
  receiverName: "",
  receiverMobile: "",
  houseDoorFlatNumber: "",
  landmarkLocality: "",
  areaNameNumber: "",
  city: "",
  postalPinCode: "",
  isDefault: false,
};

function Profile() {
  const { user, refreshProfile } = useAuth();
  const [profile, setProfile] = useState(user);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState({ type: "", text: "" });
  const [form, setForm] = useState({});
  const [pw, setPw] = useState({ oldPassword: "", newPassword: "", confirm: "" });
  const [addresses, setAddresses] = useState([]);
  const [addrForm, setAddrForm] = useState(emptyAddress);
  const [editingAddr, setEditingAddr] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        const [me, addrs] = await Promise.all([
          userApi.profile(),
          user?.userId ? userApi.addresses(user.userId).catch(() => []) : [],
        ]);
        if (me) {
          setProfile(me);
          setForm({
            userName: me.userName || "",
            firstName: me.firstName || "",
            lastName: me.lastName || "",
            emailId: me.emailId || "",
            mobileNumber: me.mobileNumber || "",
            city: me.city || "",
            postalPinCode: me.postalPinCode || "",
          });
          if (me.userId) {
            try {
              setAddresses(await userApi.addresses(me.userId));
            } catch { /* already tried */ }
          }
        }
        if (Array.isArray(addrs) && addrs.length) setAddresses(addrs);
      } catch (err) {
        setMsg({ type: "error", text: extractErrorMessage(err, "Could not load profile.") });
      } finally {
        setLoading(false);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const flash = (type, text) => setMsg({ type, text });

  const saveProfile = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const updated = await userApi.update(profile.userId, {
        ...form,
        mobileNumber: form.mobileNumber ? Number(form.mobileNumber) : null,
        postalPinCode: form.postalPinCode ? Number(form.postalPinCode) : null,
      });
      setProfile(updated);
      await refreshProfile();
      flash("ok", "Profile updated successfully.");
    } catch (err) {
      flash("error", err.message);
    } finally {
      setSaving(false);
    }
  };

  const changePassword = async (e) => {
    e.preventDefault();
    if (pw.newPassword !== pw.confirm) {
      flash("error", "New passwords do not match.");
      return;
    }
    setSaving(true);
    try {
      await userApi.changePassword(profile.userId, {
        oldPassword: pw.oldPassword,
        newPassword: pw.newPassword,
      });
      setPw({ oldPassword: "", newPassword: "", confirm: "" });
      flash("ok", "Password changed. Please sign in again on other devices.");
    } catch (err) {
      flash("error", err.message);
    } finally {
      setSaving(false);
    }
  };

  const saveAddress = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        ...addrForm,
        receiverMobile: addrForm.receiverMobile ? Number(addrForm.receiverMobile) : null,
        postalPinCode: addrForm.postalPinCode ? Number(addrForm.postalPinCode) : null,
      };
      if (editingAddr) await userApi.updateAddress(profile.userId, editingAddr, payload);
      else await userApi.addAddress(profile.userId, payload);
      setAddresses(await userApi.addresses(profile.userId));
      setAddrForm(emptyAddress);
      setEditingAddr(null);
      flash("ok", "Address saved.");
    } catch (err) {
      flash("error", err.message);
    } finally {
      setSaving(false);
    }
  };

  const removeAddress = async (id) => {
    if (!window.confirm("Remove this address?")) return;
    try {
      await userApi.deleteAddress(profile.userId, id);
      setAddresses(await userApi.addresses(profile.userId));
      flash("ok", "Address removed.");
    } catch (err) {
      flash("error", err.message);
    }
  };

  if (loading) return <Loader />;
  if (!profile) return <div className="container"><p>Could not load profile.</p></div>;

  return (
    <div className="container profile-page">
      <h2>My Profile</h2>
      {msg.text && <p className={msg.type === "ok" ? "profile-ok" : "profile-error"}>{msg.text}</p>}

      <section className="profile-card">
        <h3>Account details</h3>
        <p className="profile-meta">
          Login ID: <strong>{profile.userLoginId}</strong> · Role: <strong>{profile.roleName}</strong> ·
          Status: <strong>{profile.status}</strong>
        </p>
        <form onSubmit={saveProfile} className="profile-grid">
          {[
            ["userName", "Display name"],
            ["firstName", "First name"],
            ["lastName", "Last name"],
            ["emailId", "Email"],
            ["mobileNumber", "Mobile"],
            ["city", "City"],
            ["postalPinCode", "Pincode"],
          ].map(([key, label]) => (
            <label key={key}>{label}
              <input
                value={form[key] ?? ""}
                onChange={(e) => setForm({ ...form, [key]: e.target.value })}
              />
            </label>
          ))}
          <button type="submit" disabled={saving}>Save changes</button>
        </form>
      </section>

      <section className="profile-card">
        <h3>Change password</h3>
        <form onSubmit={changePassword} className="profile-grid">
          <label>Current password
            <input type="password" value={pw.oldPassword}
              onChange={(e) => setPw({ ...pw, oldPassword: e.target.value })} />
          </label>
          <label>New password
            <input type="password" value={pw.newPassword}
              onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} />
          </label>
          <label>Confirm new password
            <input type="password" value={pw.confirm}
              onChange={(e) => setPw({ ...pw, confirm: e.target.value })} />
          </label>
          <button type="submit" disabled={saving}>Change password</button>
        </form>
      </section>

      <section className="profile-card">
        <h3>Delivery addresses</h3>
        {addresses.length === 0 && <p>No saved addresses yet.</p>}
        <ul className="profile-addr-list">
          {addresses.map((a) => (
            <li key={a.id}>
              <strong>{a.label}</strong> — {a.receiverName}, {a.houseDoorFlatNumber},{" "}
              {a.city} {a.postalPinCode} {a.isDefault && <em>(default)</em>}
              <span className="profile-addr-actions">
                <button type="button" onClick={() => {
                  setEditingAddr(a.id);
                  setAddrForm({
                    label: a.label || "HOME",
                    receiverName: a.receiverName || "",
                    receiverMobile: a.receiverMobile || "",
                    houseDoorFlatNumber: a.houseDoorFlatNumber || "",
                    landmarkLocality: a.landmarkLocality || "",
                    areaNameNumber: a.areaNameNumber || "",
                    city: a.city || "",
                    postalPinCode: a.postalPinCode || "",
                    isDefault: !!a.isDefault,
                  });
                }}>Edit</button>
                <button type="button" onClick={() => removeAddress(a.id)}>Remove</button>
              </span>
            </li>
          ))}
        </ul>
        <form onSubmit={saveAddress} className="profile-grid">
          <label>Label
            <select value={addrForm.label} onChange={(e) => setAddrForm({ ...addrForm, label: e.target.value })}>
              <option>HOME</option>
              <option>WORK</option>
              <option>OTHER</option>
            </select>
          </label>
          <label>Receiver name
            <input value={addrForm.receiverName} onChange={(e) => setAddrForm({ ...addrForm, receiverName: e.target.value })} />
          </label>
          <label>Receiver mobile
            <input value={addrForm.receiverMobile} onChange={(e) => setAddrForm({ ...addrForm, receiverMobile: e.target.value })} />
          </label>
          <label>House / flat
            <input value={addrForm.houseDoorFlatNumber} onChange={(e) => setAddrForm({ ...addrForm, houseDoorFlatNumber: e.target.value })} />
          </label>
          <label>Landmark
            <input value={addrForm.landmarkLocality} onChange={(e) => setAddrForm({ ...addrForm, landmarkLocality: e.target.value })} />
          </label>
          <label>Area
            <input value={addrForm.areaNameNumber} onChange={(e) => setAddrForm({ ...addrForm, areaNameNumber: e.target.value })} />
          </label>
          <label>City
            <input value={addrForm.city} onChange={(e) => setAddrForm({ ...addrForm, city: e.target.value })} />
          </label>
          <label>Pincode
            <input value={addrForm.postalPinCode} onChange={(e) => setAddrForm({ ...addrForm, postalPinCode: e.target.value })} />
          </label>
          <label>Default address
            <input type="checkbox" checked={!!addrForm.isDefault}
              onChange={(e) => setAddrForm({ ...addrForm, isDefault: e.target.checked })} />
          </label>
          <button type="submit" disabled={saving}>
            {editingAddr ? "Update address" : "Add address"}
          </button>
          {editingAddr && (
            <button type="button" onClick={() => { setEditingAddr(null); setAddrForm(emptyAddress); }}>
              Cancel
            </button>
          )}
        </form>
      </section>
    </div>
  );
}

export default Profile;
